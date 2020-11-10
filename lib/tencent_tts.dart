import 'dart:async';

import 'package:flutter/services.dart';

class TencentTts {
  static const MethodChannel _channel = const MethodChannel('tencent_tts');

  /// [Future] which invokes the platform specific method for speaking
  Future<dynamic> init(String appId, String secretId, String secretKey) => _channel.invokeMethod('init', <String, dynamic>{
        "appId": appId,
        "secretId": secretId,
        "secretKey": secretKey,
      });

  /// [Future] which invokes the platform specific method for speaking
  Future<dynamic> speak(String text) => _channel.invokeMethod('speak', text);
}
