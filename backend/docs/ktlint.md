# Kotlin 코드 스타일 검사

각 도메인의 Kotlin 코드와 테스트를 같은 규칙으로 검사합니다. Java 파일은 ktlint 검사 대상이 아닙니다.

## 팀원이 처음 해야 할 작업

1. 이 설정이 병합된 공통 브랜치를 작업 브랜치에 반영합니다.
2. IntelliJ의 Gradle 창에서 **Reload All Gradle Projects**를 실행합니다.
3. Gradle JVM이 프로젝트의 Java 25를 사용하도록 확인합니다.
4. 아래 명령을 `backend` 폴더에서 실행합니다. ktlint를 PC에 별도로 설치할 필요는 없습니다.

```powershell
# Windows PowerShell
.\gradlew.bat ktlintCheck
```

```bash
# macOS / Linux
./gradlew ktlintCheck
```

IntelliJ의 EditorConfig 지원을 켜 둡니다. 지원되는 설정은 `backend/.editorconfig`에서 가져옵니다.
IntelliJ 전용 ktlint 플러그인은 선택 사항이며, 최종 판정은 저장소의 Gradle 명령을 기준으로 합니다.

## Kotlin 빌드 설정과 병합할 때

이 설정은 Kotlin 컴파일러 버전과 Spring/JPA 플러그인을 선택하지 않습니다. 해당 설정은 팀 공통 Kotlin 빌드 작업에서 반영합니다.

- 두 PR이 `build.gradle`을 수정하더라도 Kotlin JVM 플러그인과 ktlint 플러그인 선언을 모두 유지하면 됩니다.
- Kotlin JVM 플러그인이 적용되면 ktlint가 운영·테스트 Kotlin 소스 세트를 자동으로 검사합니다.
- Kotlin 빌드 설정 반영 후 `ktlintMainSourceSetCheck`, `ktlintTestSourceSetCheck` 작업이 생성되는지 확인합니다.
- 아직 Kotlin 소스와 스크립트가 없다면 `NO-SOURCE` 또는 `SKIPPED`가 나올 수 있습니다. 이는 Kotlin 코드 검사 완료를 뜻하지 않습니다.
- `src` 아래 `.kt` 파일이 있는데 Kotlin JVM 플러그인이 없으면 `ktlintCheck`를 실패시켜 누락을 알립니다.
- 두 빌드 설정을 합친 뒤 컴파일과 기존 테스트도 별도로 실행합니다. ktlint 통과는 기능 검증을 대신하지 않습니다.

## 공통 규칙

| 항목 | 규칙 |
| --- | --- |
| 기본 스타일 | `ktlint_official` |
| 들여쓰기 | 공백 4칸 |
| 최대 줄 길이 | 120자 |
| 인코딩 / 줄바꿈 | UTF-8 / LF |
| 파일 끝 | 마지막 줄바꿈 포함 |
| import | 개별 import 사용, 와일드카드 자동 생성 제한 |
| 검사 범위 | 운영·테스트 Kotlin 소스 및 프로젝트 Kotlin 스크립트 |

Gradle 플러그인은 `14.2.0`, ktlint 엔진은 `1.7.1`로 고정합니다. Kotlin 컴파일러 버전과는 별개입니다.
규칙이나 버전 변경은 개인 IDE 설정 대신 이 저장소의 공통 설정을 변경하는 PR로 제안합니다.

null 허용 여부, 엔티티 설계, 예외 처리, API 규약은 별도로 합의해야 합니다. ktlint는 형식 규칙을 검사합니다.

## 작업 중 사용하는 명령

검사만 하려면 다음을 실행합니다.

```powershell
.\gradlew.bat ktlintCheck
```

자동 수정 가능한 위반을 고치려면 다음을 실행합니다.

```powershell
.\gradlew.bat ktlintFormat
.\gradlew.bat ktlintCheck
```

macOS/Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용합니다.
자동 수정되지 않는 위반은 출력된 파일과 줄 번호를 확인해 직접 수정합니다.
`ktlintFormat`은 여러 파일을 수정할 수 있으므로 Git diff를 확인하고 본인의 작업 범위에 필요한 변경만 커밋합니다.
다른 팀원 파일을 일괄 정렬한 변경과 기능 변경은 한 PR에 섞지 않습니다.

## PR 검사

GitHub Actions의 `Backend ktlint` 워크플로가 PR에서 `ktlintCheck`를 실행합니다.
이 작업에는 DB, Docker, JWT 시크릿이 필요하지 않습니다. CI는 파일을 자동 수정하지 않습니다.

형식 위반으로 실패하면 로컬에서 수정하고 다시 push합니다. 이 PR의 로컬 검증만으로 GitHub Actions 실행까지 성공했다고 판단하지 않습니다.
실제 워크플로 결과는 PR의 Checks에서 확인합니다.

병합 조건으로 강제하려면 저장소 관리자가 첫 실행 후 표시되는 ktlint 검사를 브랜치 보호 규칙 또는 ruleset의 필수 검사로 지정해야 합니다.

## 참고 문서

- [ktlint Gradle 플러그인](https://github.com/JLLeitschuh/ktlint-gradle)
- [Kotlin 코드 품질 도구 안내](https://kotlinlang.org/docs/jvm-code-analysis.html)
- [IntelliJ EditorConfig 지원](https://www.jetbrains.com/help/idea/editorconfig.html)
