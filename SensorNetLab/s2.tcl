# ======================================================================
# Define options
# ======================================================================
 
set val(chan)         Channel/WirelessChannel  ;# channel type
set val(prop)         Propagation/TwoRayGround ;# radio-propagation model
set val(ant)          Antenna/OmniAntenna      ;# Antenna type
set val(ll)           LL                       ;# Link layer type
set val(ifq)          Queue/DropTail/PriQueue  ;# Interface queue type
set val(ifqlen)       50                       ;# max packet in ifq
set val(netif)        Phy/WirelessPhy          ;# network interface type
set val(mac)          Mac/SMAC                 ;# MAC type
set val(rp)           DSDV                     ;# ad-hoc routing protocol 
set val(nn)           7                        ;# number of mobilenodes

set ns [new Simulator]

set nf [open s2.out w]
set wf [open s2.nam w]
$ns trace-all $nf
$ns namtrace-all-wireless $wf 500 500

set topo [new Topography]

$topo load_flatgrid 500 500

set god [create-god $val(nn)]

$ns node-config -adhocRouting $val(rp) \
                         -llType $val(ll) \
                         -macType $val(mac) \
                         -ifqType $val(ifq) \
                         -ifqLen $val(ifqlen) \
                         -antType $val(ant) \
                         -propType $val(prop) \
                         -phyType $val(netif) \
                         -topoInstance $topo \
                         -channelType $val(chan) \
			 -energyModel EnergyModel \
			 -initialEnergy 100.0 \
			 -rxPower 0.20 \
			 -txPower 0.60 \
			 -idlePower 0.025 \
                         -agentTrace ON \
                         -routerTrace ON \
                         -macTrace ON \
                         -movementTrace ON

for {set i 0} {$i < $val(nn) } {incr i} {
                set node_($i) [$ns node ]
                $node_($i) random-motion 0       ;# disable random motion
        }    

$node_(0) set X_ 5.0
$node_(0) set Y_ 105.0
$node_(0) set Z_ 0.0

$node_(1) set X_ 5.0
$node_(1) set Y_ 205.0
$node_(1) set Z_ 0.0

$node_(2) set X_ 5.0
$node_(2) set Y_ 305.0
$node_(2) set Z_ 0.0

$node_(3) set X_ 155.0
$node_(3) set Y_ 205.0
$node_(3) set Z_ 0.0

$node_(4) set X_ 305.0
$node_(4) set Y_ 105.0
$node_(4) set Z_ 0.0

$node_(5) set X_ 305.0
$node_(5) set Y_ 205.0
$node_(5) set Z_ 0.0

$node_(6) set X_ 305.0
$node_(6) set Y_ 305.0
$node_(6) set Z_ 0.0

$ns at 0.2 "$node_(0) setdest 5.0 105.0 0.0"
$ns at 0.2 "$node_(1) setdest 5.0 205.0 0.0"
$ns at 0.2 "$node_(2) setdest 5.0 305.0 0.0"
$ns at 0.2 "$node_(3) setdest 155.0 205.0 0.0"
$ns at 0.2 "$node_(4) setdest 305.0 105.0 0.0"
$ns at 0.2 "$node_(5) setdest 305.0 205.0 0.0"
$ns at 0.2 "$node_(6) setdest 305.0 305.0 0.0"

set tcp0 [new Agent/TCP]
$tcp0 set class_ 2
set sink0 [new Agent/TCPSink]
$ns attach-agent $node_(0) $tcp0
$ns attach-agent $node_(6) $sink0
$ns connect $tcp0 $sink0

set tcp1 [new Agent/TCP]
$tcp1 set class_ 2
set sink1 [new Agent/TCPSink]
$ns attach-agent $node_(1) $tcp1
$ns attach-agent $node_(5) $sink1
$ns connect $tcp1 $sink1

set tcp2 [new Agent/TCP]
$tcp2 set class_ 2
set sink2 [new Agent/TCPSink]
$ns attach-agent $node_(2) $tcp2
$ns attach-agent $node_(4) $sink2
$ns connect $tcp2 $sink2

set ftp0 [new Application/FTP]
$ftp0 attach-agent $tcp0
$ns at 0.2 "$ftp0 start" 

set ftp1 [new Application/FTP]
$ftp1 attach-agent $tcp1
$ns at 0.2 "$ftp1 start" 

set ftp2 [new Application/FTP]
$ftp2 attach-agent $tcp2
$ns at 0.2 "$ftp2 start" 

for {set i 0} {$i < $val(nn) } {incr i} {
    $ns at 70.0 "$node_($i) reset";
}

$ns at 70.0001 "stop"
$ns at 70.0002 "puts \"NS EXITING...\" ; $ns halt"

proc stop {} {
    global ns nf
    close $nf
}

$ns run

