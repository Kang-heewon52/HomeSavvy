 🏠 HomeSavvy: 인공지능 기반 스마트 홈 유지보수 솔루션

 💡 프로젝트 소개

**HomeSavvy**는 Google Gemini API를 활용하여 가정 내 소모품 및 부품의 **상태를 AI로 진단**하고, **최적의 교체 주기 예측** 및 **정확한 알림 시스템**을 통해 사용자의 능동적인 홈 유지보수 및 관리 부담을 덜어주는 안드로이드 애플리케이션입니다.

---

## ⚙️ 주요 기능 (Key Features)

### 1. AI 기반 이미지 진단 (Smart Diagnosis)
- **Gemini API 활용:** 사용자가 촬영/업로드한 사진과 맥락 정보를 Gemini 1.5 Pro 모델에 전송하여 고장 원인, 부품의 종류, 상태 진단 및 상세 가이드를 제공합니다. (멀티모달 분석)
- **부품 및 도구 추천:** 진단 결과를 기반으로 필요한 부품과 공구 목록을 제시하고, 외부 검색엔진으로 연결하여 즉시 구매를 돕습니다.

### 2. 매뉴얼 스마트 검색 (Manual Smart Search)
- 매뉴얼 사진(PDF 포함)을 업로드하면 **OCR 기술**을 통해 텍스트를 추출하고, 사용자 질문에 대해 **매뉴얼 내용만을 기반**으로 정확한 요약 답변을 제공합니다. (RAGs/검색 증강 생성 패턴 구현)

### 3. 교체 주기 예측 및 알림 (Cycle Predictor)
- 사용자 입력 기반으로 소모품 교체 주기를 예측하고, **AlarmManager**를 사용하여 예측된 날짜에 정확한 알림을 예약 및 발송합니다.
- 예약된 알림 목록을 관리하고 삭제할 수 있는 UI를 제공하며, 모든 알림 데이터는 로컬에 저장됩니다.

---

## 💻 기술 스택 (Technical Stack)

| 분류 | 기술 | 비고 |
| :--- | :--- | :--- |
| **언어** | Kotlin 1.9.x | 주 개발 언어 |
| **아키텍처** | **MVVM (Model-View-ViewModel)** | `ViewModel`을 사용한 View와 비즈니스 로직 분리 |
| **AI/API** | **Google Gemini API** | 이미지 멀티모달 분석, 텍스트 요약 및 생성 (OCR 포함) |
| **플랫폼** | Android (View System - XML) | Activity/Fragment 기반의 표준 안드로이드 UI |
| **상태 관리** | LiveData / Kotlin Coroutines | 비동기 처리 및 UI 상태 관리 |
| **데이터 저장** | **SharedPreferences (via `PreferenceManager`)** | 알림 예약 정보 및 간단한 로컬 상태 저장 |
| **알림 시스템** | AlarmManager, PendingIntent, BroadcastReceiver | 정확한 시간 알람 설정 및 Android 12+ 권한 처리 |

---

## 🛡️ 기술적 도전 및 해결 과정 (Troubleshooting)

### 1. Android 12+ (API 31) Exact Alarm 권한 문제 해결
- **문제:** 정확한 알람(`setExact`) 사용 시 Android 12(API 31, S) 이상에서 **`SecurityException`**이 발생하며 앱이 강제 종료될 위험이 있었습니다.
- **해결:** `AndroidManifest.xml`에 **`SCHEDULE_EXACT_ALARM`** 권한을 선언하고, 알람 예약 함수 내에서 **`AlarmManager.canScheduleExactAlarms()`** 메서드를 통해 권한 상태를 확인하여 예외 발생을 방지하는 안전 로직을 구현했습니다.

### 2. AndroidManifest.xml Lint Error 해결
- **문제:** 카메라 권한 (`android.permission.CAMERA`) 선언 후, 해당 하드웨어 기능에 대한 명시적인 `<uses-feature>` 태그가 누락되어 빌드 경고/오류가 발생했습니다.
- **해결:** `<uses-feature android:name="android.hardware.camera" required="false" />`를 명시하여 카메라 기능이 필수 하드웨어가 아님을 시스템에 알려, 안정적인 빌드를 확보했습니다.

---

## 🔗 설치 및 실행 방법

1. **저장소 복제:**
    ```bash
    git clone [https://github.com/Kang-heewon52/HomeSavvy.git](https://github.com/Kang-heewon52/HomeSavvy.git)
    ```
2. **Android Studio 열기:**
    - Android Studio를 실행하고 복제한 디렉토리를 엽니다.
3. **Gemini API Key 설정:**
    - 프로젝트 루트 디렉토리에 **`local.properties`** 파일을 생성합니다.
    - 파일 내부에 발급받은 **Gemini API Key**를 다음과 같이 등록합니다.
      ```properties
      GEMINI_API_KEY="YOUR_API_KEY_HERE"
      ```
4. **실행:**
    - Android 에뮬레이터 또는 실제 기기를 연결하고, 실행 버튼(▶️)을 눌러 앱을 빌드하고 실행합니다.
