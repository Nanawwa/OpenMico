import json5 #line:1
class Config :#line:4
    watchdog_address :int =0x10007000 #line:5
    uart_base :int =0x11002000 #line:6
    payload_address :int =0x100A00 #line:7
    var_0 :int =None #line:8
    var_1 :int =0xA #line:9
    payload :str #line:10
    crash_method :int =0 #line:11
    ptr_usbdl :int =None #line:12
    ptr_da :int =None #line:13
    def default (O00OO000000O000OO ,O0000O0O000OOOOO0 ):#line:15
        O00OO0000OOO0O00O =open ("default_config.json5")#line:16
        O00OO000000O000OO .from_file (O00OO0000OOO0O00O ,O0000O0O000OOOOO0 )#line:17
        O00OO0000OOO0O00O .close ()#line:18
        return O00OO000000O000OO #line:20
    def from_file (OOOOO0OOOOOOO000O ,OOO0O0OO000O0O000 ,OO00OOOO0OOO0O000 ):#line:22
        OO00OOOO0OOO0O000 =hex (OO00OOOO0OOO0O000 )#line:23
        OOO0O0OO000O0O000 =json5 .load (OOO0O0OO000O0O000 )#line:25
        if OO00OOOO0OOO0O000 in OOO0O0OO000O0O000 :#line:27
            OOOOO0OOOOOOO000O .from_dict (OOO0O0OO000O0O000 [OO00OOOO0OOO0O000 ])#line:28
        else :#line:29
            raise NotImplementedError ("Can't find {} hw_code in config".format (OO00OOOO0OOO0O000 ))#line:30
        return OOOOO0OOOOOOO000O #line:32
    def from_dict (O0OO00OO000O0OOOO ,O0O000O0OO0OO00O0 ):#line:34
        if "watchdog_address"in O0O000O0OO0OO00O0 :#line:35
            O0OO00OO000O0OOOO .watchdog_address =O0O000O0OO0OO00O0 ["watchdog_address"]#line:36
        if "uart_base"in O0O000O0OO0OO00O0 :#line:38
            O0OO00OO000O0OOOO .uart_base =O0O000O0OO0OO00O0 ["uart_base"]#line:39
        if "payload_address"in O0O000O0OO0OO00O0 :#line:41
            O0OO00OO000O0OOOO .payload_address =O0O000O0OO0OO00O0 ["payload_address"]#line:42
        if "var_0"in O0O000O0OO0OO00O0 :#line:44
            O0OO00OO000O0OOOO .var_0 =O0O000O0OO0OO00O0 ["var_0"]#line:45
        if "var_1"in O0O000O0OO0OO00O0 :#line:47
            O0OO00OO000O0OOOO .var_1 =O0O000O0OO0OO00O0 ["var_1"]#line:48
        if "crash_method"in O0O000O0OO0OO00O0 :#line:50
            O0OO00OO000O0OOOO .crash_method =O0O000O0OO0OO00O0 ["crash_method"]#line:51
        if "ptr_usbdl"in O0O000O0OO0OO00O0 :#line:53
            O0OO00OO000O0OOOO .ptr_usbdl =O0O000O0OO0OO00O0 ["ptr_usbdl"]#line:54
        if "ptr_da"in O0O000O0OO0OO00O0 :#line:56
            O0OO00OO000O0OOOO .ptr_da =O0O000O0OO0OO00O0 ["ptr_da"]#line:57
        O0OO00OO000O0OOOO .payload =O0O000O0OO0OO00O0 ["payload"]#line:59
        return O0OO00OO000O0OOOO #line:61
