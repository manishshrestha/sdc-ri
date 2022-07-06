package com.example;

import org.somda.sdc.biceps.model.participant.CodedValue;

public class Constants {

    public static final String HANDLE_LOCATIONCONTEXT = "LC.mds0";
    public static final String HANDLE_PATIENTCONTEXT = "PC.mds0";

    public static final String HANDLE_ALERT_SIGNAL = "as0.mds0";
    public static final String HANDLE_ALERT_CONDITION = "ac0.mds0";

    public static final String HANDLE_NUMERIC_DYNAMIC = "numeric.ch1.vmd0";
    public static final String HANDLE_ENUM_DYNAMIC = "enumstring2.ch0.vmd0";
    public static final String HANDLE_STRING_DYNAMIC = "string2.ch0.vmd1";
    public static final String HANDLE_WAVEFORM = "rtsa.ch0.vmd0";

    public static final String HANDLE_ACTIVATE = "actop.vmd1_sco_0";
    public static final CodedValue HANDLE_ACTIVATE_CODE = CodedValue.builder()
        .withCode("196279")
        .withCodingSystem("urn:oid:1.2.840.10004.1.1.1.0.0.1")
        .build();

    public static final String HANDLE_SET_VALUE = "numeric.ch0.vmd1_sco_0";
    public static final CodedValue HANDLE_SET_VALUE_CODE = CodedValue.builder()
        .withCode("196276")
        .withCodingSystem("urn:oid:1.2.840.10004.1.1.1.0.0.1")
        .build();

    public static final String HANDLE_SET_STRING_ENUM = "enumstring.ch0.vmd1_sco_0";
    public static final CodedValue HANDLE_SET_STRING_ENUM_CODE = CodedValue.builder()
        .withCode("196277")
        .withCodingSystem("urn:oid:1.2.840.10004.1.1.1.0.0.1")
        .build();

    public static final String HANDLE_SET_STRING = "string.ch0.vmd1_sco_0";
    public static final CodedValue HANDLE_SET_STRING_CODE = CodedValue.builder()
        .withCode("196278")
        .withCodingSystem("urn:oid:1.2.840.10004.1.1.1.0.0.1")
        .build();

    public static final String HANDLE_NUMERIC_SETTABLE = "numeric.ch0.vmd1";
    public static final String HANDLE_ENUM_SETTABLE = "enumstring.ch0.vmd1";
    public static final String HANDLE_STRING_SETTABLE = "string.ch0.vmd1";

    public static final String DEFAULT_IP = "127.0.0.1";
    public static final String DEFAULT_FACILITY = "r_fac";
    public static final String DEFAULT_BED = "r_bed";
    public static final String DEFAULT_POC = "r_poc";
    public static final String DEFAULT_REPORT_TIMEOUT = "30";

}
