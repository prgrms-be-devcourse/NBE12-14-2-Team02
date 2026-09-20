# 비용 등록 API

POST /api/meetings/{meetingId}/expenses
Content-Type: application/json

균등 분담 예:
```json
{
  "title": "활동 후 식사",
  "amount": 10050,
  "memo": "음료 포함",
  "splitMode": "EQUAL",
  "roundingUnit": 100,
  "remainderMemberId": 2,
  "participants": [{"memberId": 1}, {"memberId": 2}, {"memberId": 3}]
}
```
결과: 1번 3300원, 2번 3450원, 3번 3300원. 합계 10050원.
단위는 1/10/100/1000원이고 생략 시 1원입니다.
나머지가 있으면 remainderMemberId가 필수이며 지출 참여자여야 합니다.
랜덤 선정을 원하면 remainderMemberId를 생략하고 randomRemainder: true를 추가합니다.
직접 지정과 랜덤은 동시에 사용할 수 없고, randomRemainder 생략 시 기존 직접 지정 방식입니다.
랜덤은 해당 지출 참여자 중 한 명을 같은 확률로 선택하고, 실제 선정 ID와 부담액을 함께 저장합니다.
나머지가 0원이면 추첨하지 않으며 응답의 remainderMemberId는 null입니다.
위의 remainderMemberId 필수 조건은 직접 지정 방식에서만 적용됩니다.
등록 요청을 새로 전송하면 별도의 추첨이 이루어집니다. 요청 재시도 중복 방지는 아직 구현하지 않았습니다.
금액을 선택 단위로 내림한 나머지 전부를 해당 참여자가 부담합니다.
선택한 단위보다 총액이 작으면 다른 참여자는 0원을 부담할 수도 있습니다.

금액 지정 예:
```json
{
  "title": "식사",
  "amount": 50000,
  "splitMode": "EXACT",
  "participants": [{"memberId": 1, "amount": 10000}, {"memberId": 2, "amount": 40000}]
}
```
EXACT에는 단위/차액 담당자를 지정하지 않습니다.
개인 부담액은 0원 이상 정수이며 합계가 지출 금액과 같아야 합니다.
전체 모임원 또는 일부 모임원을 실제 참여자 ID 목록으로 전달합니다.
참여자 ID는 User ID가 아닌 MeetingMember ID입니다.

성공: 201, 기존 ApiResponse 형식으로 비용 ID/결제자/금액/메모/분담 조건/개인 부담액을 반환합니다.
개인 부담액은 expense_shares 테이블에 저장됩니다.
기존 지출의 participantIds만으로는 과거 지정 부담액을 알 수 없어 기존 행의 부담액을 임의 보정하지 않습니다.

비용 전용 예외 처리 연동은 보류했습니다. 입력 검증은 유지하고 기본 Java 예외를 사용합니다.
전용 상태 코드 매핑이 없어 서비스/도메인의 기본 예외는 기존 공통 처리기에서 500으로 응답합니다.
공통 예외 파일과 application/config를 수정하지 않습니다.
사진 업로드·최종 정산·계좌 API는 이번 작업 범위가 아닙니다.
현재 SettlementCalculator는 기존 균등 계산 골격이므로 이 등록 결과를 최종 정산에 쓰려면
추후 저장된 participantAmounts를 합산하는 방식으로 연결해야 합니다. 이번에는 정산 코드를 수정하지 않습니다.

## 인증 연결
기존 MeetingAccessPort를 구현한 팀원 Bean이 필요합니다.
Principal에서 실제 로그인 사용자를 확인해 해당 모임원/열린 모임 여부를 반환해야 합니다.
클라이언트가 결제자 ID를 선택해 다른 사람을 사칭하는 임시 API는 만들지 않습니다.
미로그인, 비모임원, 종료된 모임, 연동 Bean 미준비 시 등록을 차단합니다. 상태 코드 구분은 추후 예외 처리 구현 시 반영합니다.
실제 모임장 판정과 모임 상태는 어댑터 담당 영역입니다.

## 영수증 저장 방식
1. DB 원본 저장: ExpenseReceipt.content처럼 이미지 바이트를 BLOB에 저장.
   비용과 같은 DB 트랜잭션으로 관리하기 쉽지만 DB 용량/백업 비용이 증가합니다.
2. 파일 별도 저장: 서버의 영구 볼륨 또는 오브젝트 저장소에 원본을 저장하고
   DB에는 receipts/UUID.jpg 같은 키를 보관.
   이미지 때문에 DB가 커지는 것을 줄일 수 있지만 파일 권한/백업/삭제/실패 시 정리가 필요합니다.
   사용자 컴퓨터의 C:\\... 경로를 서버 DB에 기록하는 것만으로는 업로드되지 않습니다.
   로컬 서버 디스크를 쓰면 재배포 시 보존되는 저장 공간을 별도로 구성해야 합니다.

사진 저장 방식은 설명만 하고 실제 업로드는 다음 단계로 미룹니다.
