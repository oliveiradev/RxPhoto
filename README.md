# Rx2Photo
[![](https://jitpack.io/v/oliveiradev/RxPhoto.svg)](https://jitpack.io/#oliveiradev/RxPhoto)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxPhoto-green.svg?style=true)](https://android-arsenal.com/details/1/3870)

A simple library for delivery bitmaps using reactive approach.

##Usage

Is very simple, when you need to get a picture of gallery or take a picture, use that:

* Get bitmap
```java

 Rx2Photo.with(this)
 		.requestBitmap(TypeRequest.GALLERY)
		// define your schedulers if necessary
		.doOnNext((bitmap) -> { 
			// lambda way
          		//your picture in bitmap format
       		})
		.subscribe();



```

* Get Uri
```java
Rx2Photo.with(this)
		.requestUri(TypeRequest.GALLERY)
		// define your schedulers if necessary
		.doOnNext((uri) -> { 
			// lambda way
          		//your picture in bitmap format
       		})
		.subscribe();



```

* Get Thumbnails
```java
Rx2Photo.with(this)
		.requestThumbnails(TypeRequest.GALLERY,
		new Pair(60, 60), new Pair(120, 120), new Pair(240, 240))
		// define your schedulers if necessary
		.doOnNext((uri) -> { 
			// that guy go propagate call three times, the number of your sizes
			// lambda way
          		//your picture in bitmap format
       		})
		.subscribe();



```

* Get bitmap and transform to thumbnail
```java
Rx2Photo.with(this)
		.requestBitmap(TypeRequest.GALLERY)
		// define your schedulers if necessary
		// original bitmap
		.doOnNext((bitmap) -> { 
			// lambda way
			//your picture in bitmap format
		})
		// if the ordernation not important using flatMap else using concatMap
		.flatMap(RxPhoto.transformToThumbnail(new Pair(240, 240), new Pair(120, 120), new Pair(60, 60)))
		.doOnNext((uri) -> { 
			// that guy go propagate call three times, the number of your sizes
			// lambda way
			//your picture in bitmap format
		})
		.subscribe();



```

If you need get a picture use `TypeRequest.GALLERY` if you need take a picture use `TypeRequest.CAMERA`

# Import dependency 

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
	compile 'com.github.oliveiradev:RxPhoto:$latest_version'
}
```


## Sample

The sample is on `app` module

## Developed by 

* Felipe Oliveira

Gmail: [felipe.oliveiradev@gmail.com](felipe.oliveiradev@gmail.com)

[Add me to Linkedin](https://br.linkedin.com/in/felipe-oliveira-03334b5b)

## Contribuitors

[Angelo Moroni](https://github.com/chemickypes)

[maciekczwa](https://github.com/maciekczwa)

## Libraries used

[RxJava](https://github.com/ReactiveX/RxJava)

[RxAndroid](https://github.com/ReactiveX/RxAndroid)

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

