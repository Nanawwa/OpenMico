## How to get an OTA link？
- This is an experimental algorithm, don't have too much hope for it :D

-----

- Before you start, you need to know the model, version, and update channel.
```
Explanation about the update channel:
There are three available channels, 
release (user-release), current (factory-debug), stable (user-debug). 
You can choose arbitrarily from these.
```
- Let's learn through an example.
```
Suppose you need the data of version 2.30.4 of model LX04,
And 2.30.4 is a release version,
Your model is LX04, and the update channel is release.
```
- What do we know so far?
```
model: LX04
channel: release
version: 2.30.4
```
- Let's use these to get a link.
```
Prepare a string of text: 
"channel=<channel/>&filterID=<sn/>&locale=zh_CN&model=<model/>&time=1617368871545&version=<version/>&8007236f-a2d6-4847-ac83-c49395ad6d65"
```
```
Here you need to replace the required <channel/> and <version/> with the previous content. <sn/> is an optional item.
```
```
When the replacement is complete, save this text.
```
```
Now, we need to get the BASE64 value of this string of text.
When you don’t fill in the SN, according to the previous content, we can get: 
"Y2hhbm5lbD1yZWxlYXNlJmZpbHRlcklEPSZsb2NhbGU9emhfQ04mbW9kZWw9TFgwNCZ0aW1lPTE2MTczNjg4NzE1NDUmdmVyc2lvbj0yLjMwLjQmODAwNzIzNmYtYTJkNi00ODQ3LWFjODMtYzQ5Mzk1YWQ2ZDY1"
```
```
After that, we need the uppercase MD5 value of this string of BASE64 (the length of 16 will not be mandatory)
You will get "D0EC04694F3048B18286A8E90C8E2B91".
```
```
After this, you need another string of text:
"http://api.miwifi.com/rs/grayupgrade/v2/<model/>?model=<model/>&version=<version/>&channel=<channel/>&filterID=<sn/>&locale=zh_CN&time= 1617368871545&s=<code/>"
```
```
Similar to the previous steps: 
you need to replace <model/>, <version/>, <channel/> with your own custom content. (<sn/> is also optional.)
```
```
Among them, <code/> is replaced with the previously calculated MD5.
```
```
Finally, you get a link contains all metadata for that version:
"http://api.miwifi.com/rs/grayupgrade/v2/LX04?model=LX04&version=2.30.4&channel=release&filterID=&locale=zh_CN&time=1617368871545&s=D0EC04694F3048B18286A8E90C8E2B91"
```