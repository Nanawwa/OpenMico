#!/bin/python3
from src .exploit import exploit #line:3
from src .common import from_bytes ,to_bytes #line:4
from src .config import Config #line:5
from src .device import Device #line:6
from src .logger import log #line:7
from src .bruteforce import bruteforce #line:8
import argparse #line:10
import os #line:11
DEFAULT_CONFIG ="default_config.json5"#line:13
PAYLOAD_DIR ="payloads/"#line:14
DEFAULT_PAYLOAD ="generic_dump_payload.bin"#line:15
DEFAULT_DA_ADDRESS =0x200D00 #line:16
def main ():#line:19
    O00O000O0O00OO0OO =argparse .ArgumentParser ()#line:20
    O00O000O0O00OO0OO .add_argument ("-c","--config",help ="Device config")#line:21
    O00O000O0O00OO0OO .add_argument ("-t","--test",help ="Testmode",const ="0x9900",nargs ='?')#line:22
    O00O000O0O00OO0OO .add_argument ("-w","--watchdog",help ="Watchdog address(in hex)")#line:23
    O00O000O0O00OO0OO .add_argument ("-u","--uart",help ="UART base address(in hex)")#line:24
    O00O000O0O00OO0OO .add_argument ("-v","--var_1",help ="var_1 value(in hex)")#line:25
    O00O000O0O00OO0OO .add_argument ("-a","--payload_address",help ="payload_address value(in hex)")#line:26
    O00O000O0O00OO0OO .add_argument ("-p","--payload",help ="Payload to use")#line:27
    O00O000O0O00OO0OO .add_argument ("-f","--force",help ="Force exploit on insecure device",action ="store_true")#line:28
    O00O000O0O00OO0OO .add_argument ("-n","--no_handshake",help ="Skip handshake",action ="store_true")#line:29
    O00O000O0O00OO0OO .add_argument ("-m","--crash_method",help ="Method to use for crashing preloader (0, 1, 2)",type =int )#line:30
    O00O000O0O00OO0OO .add_argument ("-k","--kamakiri",help ="Force use of kamakiri",action ="store_true")#line:31
    OOO00O0000000O0O0 =O00O000O0O00OO0OO .parse_args ()#line:32
    if OOO00O0000000O0O0 .config :#line:34
        if not os .path .exists (OOO00O0000000O0O0 .config ):#line:35
            raise RuntimeError ("Config file {} doesn't exist".format (OOO00O0000000O0O0 .config ))#line:36
    elif not os .path .exists (DEFAULT_CONFIG ):#line:37
        raise RuntimeError ("Default config is missing")#line:38
    O0O0O00O0OOO0OOOO =Device ().find ()#line:40
    O00O0OOOOO00O0O0O ,OOO0OOO0OOOO0000O ,O0O0O0000OOO000OO ,OOO0000O00OO000OO =get_device_info (O0O0O00O0OOO0OOOO ,OOO00O0000000O0O0 )#line:42
    while O0O0O00O0OOO0OOOO .preloader :#line:44
        O0O0O00O0OOO0OOOO =crash_preloader (O0O0O00O0OOO0OOOO ,O00O0OOOOO00O0O0O )#line:45
        O00O0OOOOO00O0O0O ,OOO0OOO0OOOO0000O ,O0O0O0000OOO000OO ,OOO0000O00OO000OO =get_device_info (O0O0O00O0OOO0OOOO ,OOO00O0000000O0O0 )#line:46
    log ("Disabling watchdog timer")#line:48
    O0O0O00O0OOO0OOOO .write32 (O00O0OOOOO00O0O0O .watchdog_address ,0x22000064 )#line:49
    if O0O0O00O0OOO0OOOO .libusb0 :#line:51
        OOO00O0000000O0O0 .kamakiri =True #line:52
    O00O0OO000O00OOOO ="bootrom_"+hex (OOO0000O00OO000OO )[2 :]+".bin"#line:54
    if OOO00O0000000O0O0 .test and not OOO00O0000000O0O0 .kamakiri :#line:56
        OOO000OOOOO0O0OOO =int (OOO00O0000000O0O0 .test ,16 )#line:57
        O0O00OOO0OO00O0O0 =False #line:58
        while not O0O00OOO0OO00O0O0 :#line:59
            log ("Test mode, testing "+hex (OOO000OOOOO0O0OOO )+"...")#line:60
            O0O00OOO0OO00O0O0 ,OOO000OOOOO0O0OOO =bruteforce (O0O0O00O0OOO0OOOO ,O00O0OOOOO00O0O0O ,OOO000OOOOO0O0OOO )#line:61
            O0O0O00O0OOO0OOOO .dev .close ()#line:62
            reconnect_message ()#line:63
            O0O0O00O0OOO0OOOO =Device ().find (wait =True )#line:64
            O0O0O00O0OOO0OOOO .handshake ()#line:65
            while O0O0O00O0OOO0OOOO .preloader :#line:66
                O0O0O00O0OOO0OOOO =crash_preloader (O0O0O00O0OOO0OOOO ,O00O0OOOOO00O0O0O )#line:67
                O0O0O00O0OOO0OOOO .handshake ()#line:68
        log ("Found "+hex (OOO000OOOOO0O0OOO )+", dumping bootrom to {}".format (O00O0OO000O00OOOO ))#line:69
        open (O00O0OO000O00OOOO ,"wb").write (bruteforce (O0O0O00O0OOO0OOOO ,O00O0OOOOO00O0O0O ,OOO000OOOOO0O0OOO ,True ))#line:70
        exit (0 )#line:71
    if OOO0OOO0OOOO0000O or O0O0O0000OOO000OO or OOO00O0000000O0O0 .force :#line:73
        log ("Disabling protection")#line:74
        OO0O00O0O0O00OO00 =prepare_payload (O00O0OOOOO00O0O0O )#line:76
        OO0O0OOOO0000OO0O =exploit (O0O0O00O0OOO0OOOO ,O00O0OOOOO00O0O0O ,OO0O00O0O0O00OO00 ,OOO00O0000000O0O0 )#line:78
        if OOO00O0000000O0O0 .test :#line:79
            while not OO0O0OOOO0000OO0O :#line:80
                O0O0O00O0OOO0OOOO .dev .close ()#line:81
                O00O0OOOOO00O0O0O .var_1 +=1 #line:82
                log ("Test mode, testing "+hex (O00O0OOOOO00O0O0O .var_1 )+"...")#line:83
                reconnect_message ()#line:84
                O0O0O00O0OOO0OOOO =Device ().find (wait =True )#line:85
                O0O0O00O0OOO0OOOO .handshake ()#line:86
                while O0O0O00O0OOO0OOOO .preloader :#line:87
                    O0O0O00O0OOO0OOOO =crash_preloader (O0O0O00O0OOO0OOOO ,O00O0OOOOO00O0O0O )#line:88
                    O0O0O00O0OOO0OOOO .handshake ()#line:89
                OO0O0OOOO0000OO0O =exploit (O0O0O00O0OOO0OOOO ,O00O0OOOOO00O0O0O ,OO0O00O0O0O00OO00 ,OOO00O0000000O0O0 )#line:90
    else :#line:91
        log ("Insecure device, sending payload using send_da")#line:92
        if not OOO00O0000000O0O0 .payload :#line:94
            O00O0OOOOO00O0O0O .payload =DEFAULT_PAYLOAD #line:95
        if not OOO00O0000000O0O0 .payload_address :#line:96
            O00O0OOOOO00O0O0O .payload_address =DEFAULT_DA_ADDRESS #line:97
        OO0O00O0O0O00OO00 =prepare_payload (O00O0OOOOO00O0O0O )#line:99
        OO0O00O0O0O00OO00 +=b'\x00'*0x100 #line:101
        O0O0O00O0OOO0OOOO .send_da (O00O0OOOOO00O0O0O .payload_address ,len (OO0O00O0O0O00OO00 ),0x100 ,OO0O00O0O0O00OO00 )#line:103
        O0O0O00O0OOO0OOOO .jump_da (O00O0OOOOO00O0O0O .payload_address )#line:104
        OO0O0OOOO0000OO0O =O0O0O00O0OOO0OOOO .read (4 )#line:106
    if OO0O0OOOO0000OO0O ==to_bytes (0xA1A2A3A4 ,4 ):#line:108
        log ("Protection disabled")#line:109
    elif OO0O0OOOO0000OO0O ==to_bytes (0xC1C2C3C4 ,4 ):#line:110
        dump_brom (O0O0O00O0OOO0OOOO ,O00O0OO000O00OOOO )#line:111
    elif OO0O0OOOO0000OO0O ==to_bytes (0x0000C1C2 ,4 )and O0O0O00O0OOO0OOOO .read (4 )==to_bytes (0xC1C2C3C4 ,4 ):#line:112
        dump_brom (O0O0O00O0OOO0OOOO ,O00O0OO000O00OOOO ,True )#line:113
    elif OO0O0OOOO0000OO0O !=b'':#line:114
        raise RuntimeError ("Unexpected result {}".format (OO0O0OOOO0000OO0O .hex ()))#line:115
    else :#line:116
        log ("Payload did not reply")#line:117
    O0O0O00O0OOO0OOOO .close ()#line:119
def reconnect_message ():#line:121
    print ("")#line:122
    print ("Please reconnect device in bootrom mode")#line:123
    print ("")#line:124
def dump_brom (OOO000OO0O0000O0O ,O0O0OOOOO0O000000 ,word_mode =False ):#line:126
    log ("Found send_dword, dumping bootrom to {}".format (O0O0OOOOO0O000000 ))#line:127
    with open (O0O0OOOOO0O000000 ,"wb")as OO0O0OO0O0O0OOO00 :#line:129
        if word_mode :#line:130
            for OO0OOOOOOO0O00000 in range (0x20000 //4 ):#line:131
                OOO000OO0O0000O0O .read (4 )#line:132
                OO0O0OO0O0O0OOO00 .write (OOO000OO0O0000O0O .read (4 ))#line:133
        else :#line:134
            OO0O0OO0O0O0OOO00 .write (OOO000OO0O0000O0O .read (0x20000 ))#line:135
def prepare_payload (O00OO00OOOO0OO0O0 ):#line:138
    with open (PAYLOAD_DIR +O00OO00OOOO0OO0O0 .payload ,"rb")as OO0OO0OOOO0O0O000 :#line:139
        OO0OO0OOOO0O0O000 =OO0OO0OOOO0O0O000 .read ()#line:140
    OO0OO0OOOO0O0O000 =bytearray (OO0OO0OOOO0O0O000 )#line:143
    if from_bytes (OO0OO0OOOO0O0O000 [-4 :],4 ,'<')==0x10007000 :#line:144
        OO0OO0OOOO0O0O000 [-4 :]=to_bytes (O00OO00OOOO0OO0O0 .watchdog_address ,4 ,'<')#line:145
    if from_bytes (OO0OO0OOOO0O0O000 [-8 :][:4 ],4 ,'<')==0x11002000 :#line:146
        OO0OO0OOOO0O0O000 [-8 :]=to_bytes (O00OO00OOOO0OO0O0 .uart_base ,4 ,'<')+OO0OO0OOOO0O0O000 [-4 :]#line:147
    OO0OO0OOOO0O0O000 =bytes (OO0OO0OOOO0O0O000 )#line:148
    while len (OO0OO0OOOO0O0O000 )%4 !=0 :#line:150
        OO0OO0OOOO0O0O000 +=to_bytes (0 )#line:151
    return OO0OO0OOOO0O0O000 #line:153
def get_device_info (O00000OOOO00O0OO0 ,OO0000O0000OOOO00 ):#line:156
    if not OO0000O0000OOOO00 .no_handshake :#line:157
        O00000OOOO00O0OO0 .handshake ()#line:158
    O0O000000OO0O000O =O00000OOOO00O0OO0 .get_hw_code ()#line:160
    O00OOO0OO00OO0O0O ,O0OO0OOO0OO0OO0O0 ,OO0O000OO0OOOOOOO =O00000OOOO00O0OO0 .get_hw_dict ()#line:161
    O000O0OO0O0O00O0O ,O0O00O0OO0OO0O0O0 ,O0000000OO0OOO0OO =O00000OOOO00O0OO0 .get_target_config ()#line:162
    if OO0000O0000OOOO00 .config :#line:164
        O00O0000000O0OOOO =open (OO0000O0000OOOO00 .config )#line:165
        O00O00OOO0OO00000 =Config ().from_file (O00O0000000O0OOOO ,O0O000000OO0O000O )#line:166
        O00O0000000O0OOOO .close ()#line:167
    else :#line:168
        try :#line:169
            O00O00OOO0OO00000 =Config ().default (O0O000000OO0O000O )#line:170
        except NotImplementedError as OO0OO0OOO0OO0OOOO :#line:171
            if OO0000O0000OOOO00 .test :#line:172
                O00O00OOO0OO00000 =Config ()#line:173
                log (OO0OO0OOO0OO0OOOO )#line:175
            else :#line:176
                raise OO0OO0OOO0OO0OOOO #line:177
    if OO0000O0000OOOO00 .test :#line:179
        O00O00OOO0OO00000 .payload =DEFAULT_PAYLOAD #line:180
    if OO0000O0000OOOO00 .var_1 :#line:181
        O00O00OOO0OO00000 .var_1 =int (OO0000O0000OOOO00 .var_1 ,16 )#line:182
    if OO0000O0000OOOO00 .watchdog :#line:183
        O00O00OOO0OO00000 .watchdog_address =int (OO0000O0000OOOO00 .watchdog ,16 )#line:184
    if OO0000O0000OOOO00 .uart :#line:185
        O00O00OOO0OO00000 .uart_base =int (OO0000O0000OOOO00 .uart ,16 )#line:186
    if OO0000O0000OOOO00 .payload_address :#line:187
        O00O00OOO0OO00000 .payload_address =int (OO0000O0000OOOO00 .payload_address ,16 )#line:188
    if OO0000O0000OOOO00 .payload :#line:189
        O00O00OOO0OO00000 .payload =OO0000O0000OOOO00 .payload #line:190
    if OO0000O0000OOOO00 .crash_method :#line:191
        O00O00OOO0OO00000 .crash_method =OO0000O0000OOOO00 .crash_method #line:192
    if not os .path .exists (PAYLOAD_DIR +O00O00OOO0OO00000 .payload ):#line:195
        raise RuntimeError ("Payload file {} doesn't exist".format (PAYLOAD_DIR +O00O00OOO0OO00000 .payload ))#line:196
    print ()#line:198
    log ("Reading device information...")#line:199
    log ("Device hw code: {}".format (hex (O0O000000OO0O000O )))#line:200
    if format (hex (O0O000000OO0O000O ))!="0x8167":#line:201
    		log ("The connected device is not supported")#line:202
    		sys .exit (1 )#line:203
    log ("Device hw sub code: {}".format (hex (O00OOO0OO00OO0O0O )))#line:204
    log ("Device hw version: {}".format (hex (O0OO0OOO0OO0OO0O0 )))#line:205
    log ("Device sw version: {}".format (hex (OO0O000OO0OOOOOOO )))#line:206
    log ("Device secure boot: {}".format (O000O0OO0O0O00O0O ))#line:207
    log ("Device serial link authorization: {}".format (O0O00O0OO0OO0O0O0 ))#line:208
    log ("Device download agent authorization: {}".format (O0000000OO0OOO0OO ))#line:209
    print ()#line:210
    return O00O00OOO0OO00000 ,O0O00O0OO0OO0O0O0 ,O0000000OO0OOO0OO ,O0O000000OO0O000O #line:212
def crash_preloader (O0OOO0000O0OOO0OO ,OO0O00O0O000O0O0O ):#line:214
    print ("")#line:215
    log ("Found device in preloader mode, trying to crash...")#line:216
    print ("")#line:217
    if OO0O00O0O000O0O0O .crash_method ==0 :#line:218
        try :#line:219
            O00OOO000OOO0OO00 =b'\x00\x01\x9F\xE5\x10\xFF\x2F\xE1'+b'\x00'*0x110 #line:220
            O0OOO0000O0OOO0OO .send_da (0 ,len (O00OOO000OOO0OO00 ),0 ,O00OOO000OOO0OO00 )#line:221
            O0OOO0000O0OOO0OO .jump_da (0 )#line:222
        except RuntimeError as OO000O00OOOOO00OO :#line:223
            log (OO000O00OOOOO00OO )#line:224
            print ("")#line:225
    elif OO0O00O0O000O0O0O .crash_method ==1 :#line:226
        O00OOO000OOO0OO00 =b'\x00'*0x100 #line:227
        O0OOO0000O0OOO0OO .send_da (0 ,len (O00OOO000OOO0OO00 ),0x100 ,O00OOO000OOO0OO00 )#line:228
        O0OOO0000O0OOO0OO .jump_da (0 )#line:229
    elif OO0O00O0O000O0O0O .crash_method ==2 :#line:230
        O0OOO0000O0OOO0OO .read32 (0 )#line:231
    O0OOO0000O0OOO0OO .dev .close ()#line:233
    O0OOO0000O0OOO0OO =Device ().find ()#line:235
    return O0OOO0000O0OOO0OO #line:237
if __name__ =="__main__":#line:240
    main ()#line:241
