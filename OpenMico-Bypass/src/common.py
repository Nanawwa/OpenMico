import struct #line:1
def raise_ (O0O0OOOOOO0OOOO00 ):#line:4
    raise O0O0OOOOOO0OOOO00 #line:5
def to_bytes (OO0O00O00O0OO0OO0 ,size =1 ,endian ='>'):#line:8
    return {1 :lambda :struct .pack (endian +'B',OO0O00O00O0OO0OO0 ),2 :lambda :struct .pack (endian +'H',OO0O00O00O0OO0OO0 ),4 :lambda :struct .pack (endian +'I',OO0O00O00O0OO0OO0 )}.get (size ,lambda :raise_ (RuntimeError ("invalid size")))()#line:13
def from_bytes (O0O00OO0O0000OOOO ,size =1 ,endian ='>'):#line:16
    return {1 :lambda :struct .unpack (endian +'B',O0O00OO0O0000OOOO )[0 ],2 :lambda :struct .unpack (endian +'H',O0O00OO0O0000OOOO )[0 ],4 :lambda :struct .unpack (endian +'I',O0O00OO0O0000OOOO )[0 ]}.get (size ,lambda :raise_ (RuntimeError ("invalid size")))()#line:21
