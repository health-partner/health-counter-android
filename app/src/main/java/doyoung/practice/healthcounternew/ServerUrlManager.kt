package doyoung.practice.healthcounternew

object ServerUrlManager {
    // 서버 URL을 저장할 전역 변수
    var serverUrl: String = "http://114.71.220.5:8080"

    // ML 서버 URL을 저장할 전역 변수
    // 실제 서버 상대 주소
    //var mlServerUrl: String = "http://192.168.2.52:5003"

    // 로컬에서 127.0.0.1로 띄울 때
    var mlServerUrl: String = "http://10.0.2.2:5003"
}
