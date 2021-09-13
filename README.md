![效果图](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/fastble_poster.png)

Thanks to the logo designed by [anharismail](https://github.com/anharismail)


# FastBle
Android Bluetooth Low Energy

- Filtering, scanning, linking, reading, writing, notification subscription and cancellation in a simple way.
- Supports acquiring signal strength and setting the maximum transmission unit.
- Support custom scan rules  
- Support multi device connections  
- Support reconnection  
- Support configuration timeout for conncet or operation  


### Preview
![Preview_1](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_1.png) 
![Preview_2](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_2.png) 
![Preview_3](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_3.png)
![Preview_4](https://github.com/Jasonchenlijian/FastBle/raw/master/preview/new_4.png)


### APK
If you want to quickly preview all the functions, you can download APK as a test tool directly.

 [FastBLE.apk](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE.apk) 


### Gradle

- Setp1: Add it in your root build.gradle at the end of repositories

        allprojects {
            repositories {
                ...
                maven { url 'https://jitpack.io' }
            }
        }


- Step2: Add the dependency

        dependencies {
            implementation 'com.github.Jasonchenlijian:FastBle:2.4.0'
        }
    
### Jar

[FastBLE-2.4.0.jar](https://github.com/Jasonchenlijian/FastBle/raw/master/FastBLE-2.4.0.jar)


## Wiki

[中文文档](https://github.com/Jasonchenlijian/FastBle/wiki)

[Android BLE开发详解和FastBle源码解析](https://www.jianshu.com/p/795bb0a08beb)



## Usage

- #### Init
    
        BleManager.getInstance().init(getApplication());

- #### Determine whether the current Android system supports BLE

        boolean isSupportBle()

- #### Open or close Bluetooth

		void enableBluetooth()
		void disableBluetooth()

- #### Initialization configuration

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
	            .setSplitWriteNum(20)
	            .setConnectOverTime(10000)
                .setOperateTimeout(5000);


## Contact
If you have problems and ideas to communicate with me, you can contact me in the following ways.

WeChat： chenlijian1216

Email： jasonchenlijian@gmail.com


## License

	   Copyright 2021 chenlijian

	   Licensed under the Apache License, Version 2.0 (the "License");
	   you may not use this file except in compliance with the License.
	   You may obtain a copy of the License at

   		   http://www.apache.org/licenses/LICENSE-2.0

	   Unless required by applicable law or agreed to in writing, software
	   distributed under the License is distributed on an "AS IS" BASIS,
	   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	   See the License for the specific language governing permissions and
	   limitations under the License.




