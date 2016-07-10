# RxPhoto
[![](https://jitpack.io/v/oliveiradev/RxPhoto.svg)](https://jitpack.io/#oliveiradev/RxPhoto)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxPhoto-green.svg?style=true)](https://android-arsenal.com/details/1/3870)

A simple library for delivery bitmaps using reactive approach.

##Usage

Is very simple, when you need to get a picture of gallery or take a picture, use that:

```java
RxPhoto.request(context,TypeRequest.GALLERY)
       .compose(Transformers.<Bitmap>applySchedeulers())
       .doOnNext((bitmap) -> {
          //your picture in bitmap format
       })
       .subscribe();
```

If you need get a picture use `TypeRequest.GALLERY` if you need take a picture use `TypeRequest.CAMERA`

#Install 

Add jitpack repositorie in your __build.gradle__ root level
```groovy
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```
and , add this dependency

```groovy
dependencies {
	compile 'com.github.oliveiradev:RxPhoto:0.1.0'
}
```


## To do
- [ ] Add bitmap compressor
- [ ] Define image resize
- [ ] Add run time permissions for android > 6


## Sample

The sample is on `app` module

#License
```
The MIT License (MIT)

Copyright (c) 2016 Felipe Oliveira

Permission is hereby granted, free of charge, to any person obtaining a 
copy of this software and associated documentation files (the "Software"), 
to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the Software is 
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included 
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

```

