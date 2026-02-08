# ResaleStore
* 목표: 게시글 기반 중고거래 서비스로, 게시글 등록/수정/삭제 및 검색·필터·정렬·상태(판매중/예약중/판매완료) 관리, 좋아요·리뷰(별점)·TalkJS 채팅을 제공하며 결제(Toss Payments)–에스크로–정산(지갑) 흐름까지 포함한 안전 거래를 구현
* 핵심 포인트: JWT(쿠키) 인증 + Spring Security, JPA 기반 데이터 처리, Redis(조회수/최근 본 글), S3 Presigned URL 업로드, Toss Payments 연동

## 1. ERD
<img width="700" height="600" alt="Untitled (1)" src="https://github.com/user-attachments/assets/683219a6-d5ac-42ea-b598-c8eae277098d" />

## 2. 주요 기능
### 회원/인증 기능
* 회원가입
  * 아이디/비밀번호 유효성 검사 및 중복 처리
  * 가입 후 자동 로그인(JWT 쿠키 발급)
* 로그인/로그아웃
  * 로그인 성공 시 JWT HttpOnly 쿠키 발급
  * JwtFilter로 요청 인증 처리
* 마이페이지
  * 내 게시글 목록
  * 내가 좋아요한 게시글 목록
  * 최근 본 게시글 목록(Redis 기반, 최신순/중복 제거)
  * 구매 내역(주문/거래 내역) 조회
  * 판매 내역(주문/거래 내역) 조회
  * 정산 내역 조회(판매자 수익 정산 히스토리)
* 프로필
  * 사용자 프로필 페이지(게시글/리뷰 탭 구성)
  * 프로필 이미지 변경
* 민감 작업 보호
  * 이메일+비밀번호 검증 후 임시 토큰 발급(예: 비밀번호 변경/계정 삭제 등 단계적 인증)
### 게시글 기능
* 게시글 목록
  * 페이지네이션
  * 카테고리/가격 범위/상태/정렬(최신/인기 등) 필터
  * 좋아요 여부, 조회수( Redis 기반 )를 함께 표시
* 게시글 추천 (Redis Sorted Set)
  * 인기 게시글 Top 5
  * 카테고리별 인기 추천: 현재 보고 있는 게시글과 같은 카테고리의 인기 매물 추천
* 검색
  * 제목 기반 검색(구현은 컨트롤러/서비스 레벨에서 지원)
* 상세 보기
  * 이미지 슬라이드/상세 정보
  * 채팅 이동, 상태 변경 등 액션 제공
* 게시글 생성
  * 제목/가격/카테고리/설명 등 게시글 정보 등록
  * S3 Presigned URL 발급 → 클라이언트가 S3에 직접 업로드 → 업로드된 이미지 URL을 서버에 저장
  * 다중 이미지 업로드
* 수정
  * 기존 이미지 삭제/신규 이미지 업로드 병행
  * 삭제 대상 이미지는 서버에 전달하여 정리
* 삭제
  * 권한 체크(작성자만)
  * S3 이미지 삭제 + 좋아요 정리 + DB 삭제 일괄 처리(공통 메서드로 분리)
### 결제 및 에스크로 기능 (안전 거래)
* Toss Payments 연동
  * Toss 결제 위젯을 통한 결제/승인 프로세스 구현
  * 클라이언트-서버 이중 검증으로 결제 위변조 방지
* 단계별 에스크로 거래
  * 결제 완료 → 배송 중 → 구매 확정 → 정산의 안전 거래 라이프사이클 관리
  * 거래 단계별 판매자/구매자 권한 분리 및 검증
* 직거래 지원
  * 구매자 선택 판매: 채팅을 통해 대화한 사용자 중 실제 구매자를 선택하여 판매 완료 처리
  * 간편 주문 생성: 결제 과정 없이 즉시 '직거래 주문(DIRECT)' 생성 및 이력 관리
* 게시글 상태 자동화
  * 결제 승인 시 게시글 예약중(Reserved) 자동 전환
  * 구매 확정 시 게시글 판매완료(Sold) 자동 처리
* 정산 및 지갑(Wallet)
  * 구매 확정 시 수수료 제외 후 판매자 지갑으로 자동 정산
  * 가상 지갑 잔액 관리 및 정산 내역 조회
* 거래 내역 관리
  * 구매/판매 내역 분리 조회
  * 진행 중인 거래 상태 확인 및 단계별 액션(배송처리/구매확정) 제공
### 좋아요 기능
* 좋아요 토글
  * 로그인 여부 확인 후 추가/삭제
  * 게시글의 likeCnt 동기화
  * 프론트에서는 아이콘 상태(채움/비움)와 카운트 즉시 업데이트
* 추천 연동
  * 좋아요 발생 시 추천 점수(인기 점수) 갱신(Redis Sorted Set 기반)
### 리뷰(거래 후기) 기능
* 리뷰 작성
  * 평점(별점) + 텍스트 리뷰
  * 대상 사용자 프로필에서 작성
* 리뷰 삭제
  * 작성자만 삭제 가능(일반 사용자)
  * 관리자 삭제는 별도 엔드포인트로 처리
  * 삭제 성공/실패 메시지를 응답 본문으로 내려 프론트에서 alert 처리
### 관리자 기능
* 대시보드
  * 사용자 수/게시글 수/리뷰 수/좋아요 수 등 통계
* 사용자 관리
  * 목록/검색
  * 검색(사용자명 또는 이메일)
* 게시글 관리
  * 검색/필터/페이지네이션
  * 관리자 권한으로 삭제(RESTful DELETE + fetch)
* 리뷰 관리
  * 목록
### 채팅 기능
* TalkJS 연동
  * 사용자/게시글 작성자 정보를 기반으로 1:1 대화 생성 또는 inbox 표시

## 3. 보안/품질
* SQL Injection 점검
* Repository에서 파라미터 바인딩 방식(JPA)을 사용하여 SQL Injection 위험을 낮춤
* CSRF
* HttpSessionCsrfTokenRepository로 토큰 발급
* JS fetch에서 meta의 CSRF 값을 읽어 헤더로 전송
* 권한/인가
* 관리자: /admin/** 보호
* 일반 기능: @PreAuthorize("isAuthenticated()") 기반 보호
* 프로파일 분리(dev/prod)
* prod: Swagger 비활성화, ddl-auto validate, SQL 로그 최소화

## 4. API 명세서
[API 명세서](https://thinkable-earthquake-735.notion.site/Resale-Store-API-2e7a5166124c800c861dfa772bbac7c0?source=copy_link)

## 5. 트러블 슈팅
[트러블 슈팅](https://thinkable-earthquake-735.notion.site/2eea5166124c80f88b1bcf93f60225f0?source=copy_link)

## 6. 기술스택
### 백엔드
![Java](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.5.7-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring MVC](https://img.shields.io/badge/Spring%20MVC-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-CA4245?style=for-the-badge)
![Redis](https://img.shields.io/badge/Redis%20(Lettuce)-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazon-s3&logoColor=white)
![AWS](https://img.shields.io/badge/Presigned%20URL-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger%20(Springdoc)-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Toss Payments](https://img.shields.io/badge/Toss%20Payments%20API-0064FF?style=for-the-badge)

### 프론트엔드
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Spring Security](https://img.shields.io/badge/Security%20Dialect-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Bootstrap](https://img.shields.io/badge/Bootstrap%205-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)
![Bootstrap Icons](https://img.shields.io/badge/Bootstrap%20Icons-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white)
![JavaScript](https://img.shields.io/badge/Vanilla%20JS-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![TalkJS](https://img.shields.io/badge/TalkJS-2E77BC?style=for-the-badge)
![Toss Payments Widget](https://img.shields.io/badge/Toss%20Payments%20Widget-0064FF?style=for-the-badge)

### Environment & Etc
![AWS EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Profile](https://img.shields.io/badge/Spring%20Profiles-dev%20%7C%20prod-6DB33F?style=for-the-badge)
