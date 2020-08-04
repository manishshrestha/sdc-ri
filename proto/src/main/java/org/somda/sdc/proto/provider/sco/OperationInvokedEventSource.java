package org.somda.sdc.proto.provider.sco;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.proto.addressing.AddressingUtil;
import org.somda.sdc.proto.mapping.message.PojoToProtoMapper;
import org.somda.sdc.proto.mapping.message.ProtoToPojoMapper;
import org.somda.sdc.proto.model.OperationInvokedReportRequest;
import org.somda.sdc.proto.model.OperationInvokedReportStream;
import org.somda.sdc.proto.model.biceps.OperationInvokedReportMsg;
import org.somda.sdc.proto.provider.EventSource;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OperationInvokedEventSource extends AbstractIdleService implements EventSource {
    private final static int QUEUE_SIZE = 100;

    private final Map<Integer, BlockingQueue<QueueItem>> subscribedInvokedReports;
    private final AtomicInteger susbcriptions;
    private final ProtoToPojoMapper fromProtoMessageMapper;
    private final PojoToProtoMapper pojoToProtoMapper;
    private final AddressingUtil addressingUtil;

    @Inject
    OperationInvokedEventSource(ProtoToPojoMapper fromProtoMessageMapper,
                                PojoToProtoMapper pojoToProtoMapper,
                                AddressingUtil addressingUtil) {
        this.fromProtoMessageMapper = fromProtoMessageMapper;
        this.pojoToProtoMapper = pojoToProtoMapper;
        this.addressingUtil = addressingUtil;
        this.subscribedInvokedReports = new ConcurrentHashMap<>();
        this.susbcriptions = new AtomicInteger(0);
    }

    public void sendNotification(String action, AbstractReport report) throws Exception {
        if (!(report instanceof OperationInvokedReport)) {
            throw new Exception("OperationInvokedEventSource can only process OperationInvokedReport instances.");
        }

        var reportMsg = pojoToProtoMapper.mapOperationInvokedReport((OperationInvokedReport)report);
        subscribedInvokedReports.values().forEach(it -> it.offer(new InvokedReportItem(action, reportMsg)));
    }

    public void handleOperationInvokedReport(OperationInvokedReportRequest request,
                                             StreamObserver<OperationInvokedReportStream> responseObserver) {
        var queue = new ArrayBlockingQueue<QueueItem>(QUEUE_SIZE);
        var subscriptionNumber = susbcriptions.incrementAndGet();
        subscribedInvokedReports.put(subscriptionNumber, queue);
        try {
            while (true) {
                var element = queue.poll();
                // this will throw and kill the subscription, should be changed at some point
                assert element != null;
                if (element instanceof QueueTerminationItem) {
                    responseObserver.onCompleted();
                    return;
                } else if (element instanceof InvokedReportItem) {
                    var reportItem = ((InvokedReportItem) element);
                    var message = OperationInvokedReportStream.newBuilder()
                            .setOperationInvoked(reportItem.getReport())
                            .setAddressing(addressingUtil.assembleAddressing(reportItem.getAction()));
                    responseObserver.onNext(message.build());
                }
            }
        } finally {
            subscribedInvokedReports.remove(subscriptionNumber);
        }
    }

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
        subscribedInvokedReports.values().forEach(it -> it.offer(new QueueTerminationItem()));
    }

    private interface QueueItem {
    }

    private class InvokedReportItem implements QueueItem {
        private final String action;
        private final OperationInvokedReportMsg report;

        InvokedReportItem(String action, OperationInvokedReportMsg report) {
            this.action = action;
            this.report = report;
        }

        public String getAction() {
            return action;
        }

        OperationInvokedReportMsg getReport() {
            return report;
        }
    }

    private class QueueTerminationItem implements QueueItem {
    }
}
