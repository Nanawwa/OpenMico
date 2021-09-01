from src .common import to_bytes ,from_bytes #line:1
from src .logger import log #line:2
import usb #line:3
import usb .backend .libusb1 #line:4
import usb .backend .libusb0 #line:5
from ctypes import c_void_p ,c_int #line:6
import array #line:7
import os #line:8
import time #line:10
BAUD =115200 #line:12
TIMEOUT =1 #line:13
VID ="0E8D"#line:14
PID ="0003"#line:15
class Device :#line:18
    def __init__ (O00OOO0OOOOOO00O0 ,port =None ):#line:19
        O00OOO0OOOOOO00O0 .udev =None #line:20
        O00OOO0OOOOOO00O0 .dev =None #line:21
        O00OOO0OOOOOO00O0 .rxbuffer =array .array ('B')#line:22
        O00OOO0OOOOOO00O0 .preloader =False #line:23
        O00OOO0OOOOOO00O0 .timeout =TIMEOUT #line:24
        O00OOO0OOOOOO00O0 .usbdk =False #line:25
        O00OOO0OOOOOO00O0 .libusb0 =False #line:26
        if os .name =='nt':#line:28
            try :#line:29
                O0000O00O000O0000 =os .path .join (os .path .abspath (os .path .dirname (__file__ )),"..")#line:30
                try :#line:31
                    os .add_dll_directory (O0000O00O000O0000 )#line:32
                except Exception :#line:33
                    pass #line:34
                os .environ ['PATH']=O0000O00O000O0000 +';'+os .environ ['PATH']#line:35
            except Exception :#line:36
                pass #line:37
    def find (O0OOO00OOO0OO0OO0 ,wait =False ):#line:39
        if O0OOO00OOO0OO0OO0 .dev :#line:40
            raise RuntimeError ("Device already found")#line:41
        try :#line:43
            O0OOO00OOO0OO0OO0 .backend =usb .backend .libusb1 .get_backend (find_library =lambda OO0OO00O00000OO0O :"libusb-1.0.dll")#line:44
            if O0OOO00OOO0OO0OO0 .backend :#line:45
                try :#line:46
                    O0OOO00OOO0OO0OO0 .backend .lib .libusb_set_option .argtypes =[c_void_p ,c_int ]#line:47
                    O0OOO00OOO0OO0OO0 .backend .lib .libusb_set_option (O0OOO00OOO0OO0OO0 .backend .ctx ,1 )#line:48
                    O0OOO00OOO0OO0OO0 .usbdk =True #line:49
                except ValueError :#line:50
                    log ("Failed enabling UsbDk mode, please use 64-Bit Python and 64-Bit UsbDk")#line:51
            else :#line:52
                O0OOO00OOO0OO0OO0 .backend =usb .backend .libusb1 .get_backend ()#line:53
        except usb .core .USBError :#line:54
            O0OOO00OOO0OO0OO0 .backend =usb .backend .libusb1 .get_backend ()#line:55
        log ("MediaTek MT8167 Generic Bypass by Yagami Ko")#line:57
        log ("Waiting for device")#line:58
        if wait :#line:59
            O0OOO00OOO0OO0OO0 .udev =usb .core .find (idVendor =int (VID ,16 ),backend =O0OOO00OOO0OO0OO0 .backend )#line:60
            while O0OOO00OOO0OO0OO0 .udev :#line:61
                time .sleep (0.25 )#line:62
                O0OOO00OOO0OO0OO0 .udev =usb .core .find (idVendor =int (VID ,16 ),backend =O0OOO00OOO0OO0OO0 .backend )#line:63
        O0OOO00OOO0OO0OO0 .udev =None #line:64
        while not O0OOO00OOO0OO0OO0 .udev :#line:65
            O0OOO00OOO0OO0OO0 .udev =usb .core .find (idVendor =int (VID ,16 ),backend =O0OOO00OOO0OO0OO0 .backend )#line:66
            if O0OOO00OOO0OO0OO0 .udev :#line:67
                break #line:68
            time .sleep (0.25 )#line:69
        log ("Found device = {0:04x}:{1:04x}".format (O0OOO00OOO0OO0OO0 .udev .idVendor ,O0OOO00OOO0OO0OO0 .udev .idProduct ))#line:71
        O0OOO00OOO0OO0OO0 .dev =O0OOO00OOO0OO0OO0 #line:72
        try :#line:74
            if O0OOO00OOO0OO0OO0 .udev .is_kernel_driver_active (0 ):#line:75
                O0OOO00OOO0OO0OO0 .udev .detach_kernel_driver (0 )#line:76
            if O0OOO00OOO0OO0OO0 .udev .is_kernel_driver_active (1 ):#line:78
                O0OOO00OOO0OO0OO0 .udev .detach_kernel_driver (1 )#line:79
        except (NotImplementedError ,usb .core .USBError ):#line:81
            pass #line:82
        try :#line:84
            O0OOO00OOO0OO0OO0 .configuration =O0OOO00OOO0OO0OO0 .udev .get_active_configuration ()#line:85
        except (usb .core .USBError ,NotImplementedError )as OO0O0OOO0OO0O0000 :#line:86
            if type (OO0O0OOO0OO0O0000 )is usb .core .USBError and OO0O0OOO0OO0O0000 .errno ==13 or type (OO0O0OOO0OO0O0000 )is NotImplementedError :#line:87
                log ("Failed to enable libusb1, is UsbDk installed?")#line:88
                log ("Falling back to libusb0 (kamakiri only)")#line:89
                O0OOO00OOO0OO0OO0 .backend =usb .backend .libusb0 .get_backend ()#line:90
                O0OOO00OOO0OO0OO0 .udev =usb .core .find (idVendor =int (VID ,16 ),backend =O0OOO00OOO0OO0OO0 .backend )#line:91
                O0OOO00OOO0OO0OO0 .libusb0 =True #line:92
            try :#line:93
                O0OOO00OOO0OO0OO0 .udev .set_configuration ()#line:94
            except AttributeError :#line:95
                log ("Failed to enable libusb0")#line:96
                exit (1 )#line:97
        if O0OOO00OOO0OO0OO0 .udev .idProduct !=int (PID ,16 ):#line:99
            O0OOO00OOO0OO0OO0 .preloader =True #line:100
        else :#line:101
            try :#line:102
                O0OOO00OOO0OO0OO0 .udev .set_configuration (1 )#line:103
                usb .util .claim_interface (O0OOO00OOO0OO0OO0 .udev ,0 )#line:104
                usb .util .claim_interface (O0OOO00OOO0OO0OO0 .udev ,1 )#line:105
            except usb .core .USBError :#line:106
                pass #line:107
        OOO0OOOOO00OO0000 =usb .util .find_descriptor (O0OOO00OOO0OO0OO0 .udev .get_active_configuration (),bInterfaceClass =0xA )#line:109
        O0OOO00OOO0OO0OO0 .ep_in =usb .util .find_descriptor (OOO0OOOOO00OO0000 ,custom_match =lambda OO0OO00O0000OO0O0 :usb .util .endpoint_direction (OO0OO00O0000OO0O0 .bEndpointAddress )==usb .util .ENDPOINT_IN )#line:110
        O0OOO00OOO0OO0OO0 .ep_out =usb .util .find_descriptor (OOO0OOOOO00OO0000 ,custom_match =lambda OOOOOO00OOO0OO0OO :usb .util .endpoint_direction (OOOOOO00OOO0OO0OO .bEndpointAddress )==usb .util .ENDPOINT_OUT )#line:111
        try :#line:113
            O0OOO00OOO0OO0OO0 .udev .ctrl_transfer (0x21 ,0x20 ,0 ,0 ,array .array ('B',to_bytes (BAUD ,4 ,'<')+b"\x00\x00\x08"))#line:114
        except usb .core .USBError :#line:115
            pass #line:116
        return O0OOO00OOO0OO0OO0 #line:118
    @staticmethod #line:120
    def check (O0O0OOOOO0OOO0000 ,O000O000O00OO0OO0 ):#line:121
        if O0O0OOOOO0OOO0000 !=O000O000O00OO0OO0 :#line:122
            if type (O0O0OOOOO0OOO0000 )==bytes :#line:123
                O0O0OOOOO0OOO0000 ="0x"+O0O0OOOOO0OOO0000 .hex ()#line:124
            else :#line:125
                O0O0OOOOO0OOO0000 =hex (O0O0OOOOO0OOO0000 )#line:126
            if type (O000O000O00OO0OO0 )==bytes :#line:128
                O000O000O00OO0OO0 ="0x"+O000O000O00OO0OO0 .hex ()#line:129
            else :#line:130
                O000O000O00OO0OO0 =hex (O000O000O00OO0OO0 )#line:131
            raise RuntimeError ("Unexpected output, expected {} got {}".format (O000O000O00OO0OO0 ,O0O0OOOOO0OOO0000 ))#line:133
    def close (O0000O000OO0O00O0 ):#line:135
        O0000O000OO0O00O0 .dev =None #line:136
        O0000O000OO0O00O0 .rxbuffer =array .array ('B')#line:137
        try :#line:138
            usb .util .release_interface (O0000O000OO0O00O0 .udev ,0 )#line:139
            usb .util .release_interface (O0000O000OO0O00O0 .udev ,1 )#line:140
        except Exception :#line:141
            pass #line:142
        if not O0000O000OO0O00O0 .usbdk :#line:143
            try :#line:144
                O0000O000OO0O00O0 .udev .reset ()#line:145
            except Exception :#line:146
                pass #line:147
        try :#line:148
            O0000O000OO0O00O0 .udev .attach_kernel_driver (0 )#line:149
        except Exception :#line:150
            pass #line:151
        try :#line:152
            O0000O000OO0O00O0 .udev .attach_kernel_driver (1 )#line:153
        except Exception :#line:154
            pass #line:155
        if not O0000O000OO0O00O0 .usbdk :#line:156
            try :#line:157
                usb .util .dispose_resources (O0000O000OO0O00O0 .udev )#line:158
            except Exception :#line:159
                pass #line:160
        O0000O000OO0O00O0 .udev =None #line:161
        time .sleep (1 )#line:162
    def handshake (O0OO00000OOO0OO0O ):#line:164
        OOO00O000O000O00O =b"\xA0\x0A\x50\x05"#line:165
        O0O0O00O0O00OO0OO =0 #line:166
        while O0O0O00O0O00OO0OO <len (OOO00O000O000O00O ):#line:167
            O0OO00000OOO0OO0O .write (OOO00O000O000O00O [O0O0O00O0O00OO0OO ])#line:168
            OO0O00OO0O0OOO0OO =O0OO00000OOO0OO0O .read (1 )#line:169
            if OO0O00OO0O0OOO0OO and OO0O00OO0O0OOO0OO [0 ]==~OOO00O000O000O00O [O0O0O00O0O00OO0OO ]&0xFF :#line:170
                O0O0O00O0O00OO0OO +=1 #line:171
            else :#line:172
                O0O0O00O0O00OO0OO =0 #line:173
    def echo (OOO00OOO0O0OOOO00 ,O0OO0OO0OOOO00OO0 ,size =1 ):#line:175
        OOO00OOO0O0OOOO00 .write (O0OO0OO0OOOO00OO0 ,size )#line:176
        OOO00OOO0O0OOOO00 .check (from_bytes (OOO00OOO0O0OOOO00 .read (size ),size ),O0OO0OO0OOOO00OO0 )#line:177
    def read (OOOOOOOOO0O00O000 ,size =1 ):#line:179
        O000O000000O0OOO0 =0 #line:180
        O00O00OO0O0OO000O =b""#line:181
        while len (OOOOOOOOO0O00O000 .rxbuffer )<size :#line:182
            try :#line:183
                OOOOOOOOO0O00O000 .rxbuffer .extend (OOOOOOOOO0O00O000 .ep_in .read (OOOOOOOOO0O00O000 .ep_in .wMaxPacketSize ,OOOOOOOOO0O00O000 .timeout *1000 ))#line:184
            except usb .core .USBError as OOOOOO0O0O00OO0O0 :#line:185
                if OOOOOO0O0O00OO0O0 .errno ==110 :#line:186
                    OOOOOOOOO0O00O000 .udev .reset ()#line:187
                break #line:188
        if size <=len (OOOOOOOOO0O00O000 .rxbuffer ):#line:189
            OO000000OO0OO0OOO =OOOOOOOOO0O00O000 .rxbuffer [:size ]#line:190
            OOOOOOOOO0O00O000 .rxbuffer =OOOOOOOOO0O00O000 .rxbuffer [size :]#line:191
        else :#line:192
            OO000000OO0OO0OOO =OOOOOOOOO0O00O000 .rxbuffer #line:193
            OOOOOOOOO0O00O000 .rxbuffer =array .array ('B')#line:194
        return bytes (OO000000OO0OO0OOO )#line:195
    def read32 (OOOOO0OOO0OOOO000 ,OOO0O00O0OO0OO000 ,size =1 ):#line:197
        O0O0O000O0000O00O =[]#line:198
        OOOOO0OOO0OOOO000 .echo (0xD1 )#line:200
        OOOOO0OOO0OOOO000 .echo (OOO0O00O0OO0OO000 ,4 )#line:201
        OOOOO0OOO0OOOO000 .echo (size ,4 )#line:202
        OOO0O0OO0000O000O =OOOOO0OOO0OOOO000 .dev .read (2 )#line:204
        if from_bytes (OOO0O0OO0000O000O ,2 )>0xff :#line:205
            raise RuntimeError ("status is {}".format (OOO0O0OO0000O000O .hex ()))#line:206
        for _O00000O0O0OO000OO in range (size ):#line:208
            O000O00OOOOOO0OO0 =from_bytes (OOOOO0OOO0OOOO000 .dev .read (4 ),4 )#line:209
            O0O0O000O0000O00O .append (O000O00OOOOOO0OO0 )#line:210
        OOO0O0OO0000O000O =OOOOO0OOO0OOOO000 .dev .read (2 )#line:212
        if from_bytes (OOO0O0OO0000O000O ,2 )>0xff :#line:213
            raise RuntimeError ("status is {}".format (OOO0O0OO0000O000O .hex ()))#line:214
        if len (O0O0O000O0000O00O )==1 :#line:217
            return O0O0O000O0000O00O [0 ]#line:218
        else :#line:219
            return O0O0O000O0000O00O #line:220
    def write (OOOOOO0000O000O0O ,O0O000OO0O00OO00O ,size =1 ):#line:222
        if type (O0O000OO0O00OO00O )!=bytes :#line:223
            O0O000OO0O00OO00O =to_bytes (O0O000OO0O00OO00O ,size )#line:224
        O0O0000O000O00O00 =0 #line:225
        while O0O0000O000O00O00 <len (O0O000OO0O00OO00O ):#line:226
            OOOOOO0000O000O0O .ep_out .write (O0O000OO0O00OO00O [O0O0000O000O00O00 :][:OOOOOO0000O000O0O .ep_out .wMaxPacketSize if len (O0O000OO0O00OO00O )-O0O0000O000O00O00 >OOOOOO0000O000O0O .ep_out .wMaxPacketSize else len (O0O000OO0O00OO00O )-O0O0000O000O00O00 ],OOOOOO0000O000O0O .timeout *1000 )#line:227
            O0O0000O000O00O00 +=OOOOOO0000O000O0O .ep_out .wMaxPacketSize #line:228
    def write32 (O0OOOOOOO0O00O0O0 ,O00O0OOO00O0OO00O ,O0OO0O00OOO0O000O ,check_status =True ):#line:230
        if not isinstance (O0OO0O00OOO0O000O ,list ):#line:232
            O0OO0O00OOO0O000O =[O0OO0O00OOO0O000O ]#line:233
        O0OOOOOOO0O00O0O0 .echo (0xD4 )#line:235
        O0OOOOOOO0O00O0O0 .echo (O00O0OOO00O0OO00O ,4 )#line:236
        O0OOOOOOO0O00O0O0 .echo (len (O0OO0O00OOO0O000O ),4 )#line:237
        O0OOOOOOO0O00O0O0 .check (O0OOOOOOO0O00O0O0 .dev .read (2 ),to_bytes (1 ,2 ))#line:239
        for O0O0OO0OOOO0OO0O0 in O0OO0O00OOO0O000O :#line:241
            O0OOOOOOO0O00O0O0 .echo (O0O0OO0OOOO0OO0O0 ,4 )#line:242
        if check_status :#line:244
            O0OOOOOOO0O00O0O0 .check (O0OOOOOOO0O00O0O0 .dev .read (2 ),to_bytes (1 ,2 ))#line:245
    def get_target_config (OO0O000OO00O0OO0O ):#line:247
        OO0O000OO00O0OO0O .echo (0xD8 )#line:248
        O0OO0OO000OOOO000 =OO0O000OO00O0OO0O .dev .read (4 )#line:250
        OOO000000O0O0OOO0 =OO0O000OO00O0OO0O .dev .read (2 )#line:251
        if from_bytes (OOO000000O0O0OOO0 ,2 )!=0 :#line:253
            raise RuntimeError ("status is {}".format (OOO000000O0O0OOO0 .hex ()))#line:254
        O0OO0OO000OOOO000 =from_bytes (O0OO0OO000OOOO000 ,4 )#line:256
        OOO0O0O0OO00O0O00 =O0OO0OO000OOOO000 &1 #line:258
        OOOO00OOO00O0OO00 =O0OO0OO000OOOO000 &2 #line:259
        O000OO000O000OOO0 =O0OO0OO000OOOO000 &4 #line:260
        return bool (OOO0O0O0OO00O0O00 ),bool (OOOO00OOO00O0OO00 ),bool (O000OO000O000OOO0 )#line:263
    def get_hw_code (OOO0OOO00000OOO0O ):#line:265
        OOO0OOO00000OOO0O .echo (0xFD )#line:266
        OOO0O00OOO0O00O0O =OOO0OOO00000OOO0O .dev .read (2 )#line:268
        OOOO00O000O0O0000 =OOO0OOO00000OOO0O .dev .read (2 )#line:269
        if from_bytes (OOOO00O000O0O0000 ,2 )!=0 :#line:271
            raise RuntimeError ("status is {}".format (OOOO00O000O0O0000 .hex ()))#line:272
        return from_bytes (OOO0O00OOO0O00O0O ,2 )#line:274
    def get_hw_dict (OO0O00000O0O0O0O0 ):#line:276
        OO0O00000O0O0O0O0 .echo (0xFC )#line:277
        OO000O000O0OOO0O0 =OO0O00000O0O0O0O0 .dev .read (2 )#line:279
        OOO0O0000OO0OO0O0 =OO0O00000O0O0O0O0 .dev .read (2 )#line:280
        O0OOOOOO0OOOOOO0O =OO0O00000O0O0O0O0 .dev .read (2 )#line:281
        OO00O00OO00O00000 =OO0O00000O0O0O0O0 .dev .read (2 )#line:282
        if from_bytes (OO00O00OO00O00000 ,2 )!=0 :#line:284
            raise RuntimeError ("status is {}".format (OO00O00OO00O00000 .hex ()))#line:285
        return from_bytes (OO000O000O0OOO0O0 ,2 ),from_bytes (OOO0O0000OO0OO0O0 ,2 ),from_bytes (O0OOOOOO0OOOOOO0O ,2 )#line:287
    def send_da (OO0000O000000O0OO ,O0O00OO0O0OOO00O0 ,OOOOOOOOO0OO000O0 ,OO000OO000000OOOO ,OOOO0OO00OO0O0OOO ):#line:289
        OO0000O000000O0OO .echo (0xD7 )#line:290
        OO0000O000000O0OO .echo (O0O00OO0O0OOO00O0 ,4 )#line:292
        OO0000O000000O0OO .echo (OOOOOOOOO0OO000O0 ,4 )#line:293
        OO0000O000000O0OO .echo (OO000OO000000OOOO ,4 )#line:294
        O0OO0000OO00O0000 =OO0000O000000O0OO .dev .read (2 )#line:296
        if from_bytes (O0OO0000OO00O0000 ,2 )!=0 :#line:298
            raise RuntimeError ("status is {}".format (O0OO0000OO00O0000 .hex ()))#line:299
        OO0000O000000O0OO .dev .write (OOOO0OO00OO0O0OOO )#line:301
        OOOO0O00O0O0OOO0O =OO0000O000000O0OO .dev .read (2 )#line:303
        O0OO0000OO00O0000 =OO0000O000000O0OO .dev .read (2 )#line:304
        if from_bytes (O0OO0000OO00O0000 ,2 )!=0 :#line:306
            raise RuntimeError ("status is {}".format (O0OO0000OO00O0000 .hex ()))#line:307
        return from_bytes (OOOO0O00O0O0OOO0O ,2 )#line:309
    def jump_da (O000OO0OOO00O00OO ,OO0O0O0OO0O0O00OO ):#line:311
        O000OO0OOO00O00OO .echo (0xD5 )#line:312
        O000OO0OOO00O00OO .echo (OO0O0O0OO0O0O00OO ,4 )#line:314
        OO0O0O00OOOOO0OOO =O000OO0OOO00O00OO .dev .read (2 )#line:316
        if from_bytes (OO0O0O00OOOOO0OOO ,2 )!=0 :#line:318
            raise RuntimeError ("status is {}".format (OO0O0O00OOOOO0OOO .hex ()))#line:319
    def cmd_da (O00O0OOOOO00000O0 ,O0O000O000OOOO00O ,OOOOOO00O0O0O00O0 ,O00O00OOOO0OOOO0O ,data =None ,check_status =True ):#line:321
        O00O0OOOOO00000O0 .echo (0xDA )#line:322
        O00O0OOOOO00000O0 .echo (O0O000O000OOOO00O ,4 )#line:324
        O00O0OOOOO00000O0 .echo (OOOOOO00O0O0O00O0 ,4 )#line:325
        O00O0OOOOO00000O0 .echo (O00O00OOOO0OOOO0O ,4 )#line:326
        OOO000OOO0O0O00OO =O00O0OOOOO00000O0 .dev .read (2 )#line:328
        if from_bytes (OOO000OOO0O0O00OO ,2 )!=0 :#line:330
            raise RuntimeError ("status is {}".format (OOO000OOO0O0O00OO .hex ()))#line:331
        if (O0O000O000OOOO00O &1 )==1 :#line:333
            O00O0OOOOO00000O0 .dev .write (data )#line:334
        else :#line:335
            data =O00O0OOOOO00000O0 .dev .read (O00O00OOOO0OOOO0O )#line:336
        if check_status :#line:338
            OOO000OOO0O0O00OO =O00O0OOOOO00000O0 .dev .read (2 )#line:339
            if from_bytes (OOO000OOO0O0O00OO ,2 )!=0 :#line:341
                raise RuntimeError ("status is {}".format (OOO000OOO0O0O00OO .hex ()))#line:342
        return data #line:344
