#!/bin/bash

apt-get update && apt-get install -y tcpdump
tcpdump udp -i lo -w udp_traffic.pcap &
