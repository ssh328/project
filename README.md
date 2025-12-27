# ResaleStore
* 목표: 게시글 기반 중고거래 서비스(검색/필터/상태관리), 좋아요·리뷰·채팅, 조회수/최근 본 글을 Redis로 최적화, 이미지 업로드를 S3 Presigned URL로 처리
* 핵심 포인트: JWT(쿠키) 인증 + Spring Security, JPA 기반 데이터 처리, Redis(조회수/최근 본 글), S3 Presigned URL 업로드

## ⚙️사용 기술 스택
### Backend
* Java 21
* Spring Boot 3.5.7, Spring MVC
* Spring Security(메서드 보안 포함), JWT(jjwt)
* Spring Data JPA(Hibernate), MySQL
* Redis(Lettuce), RedisTemplate<String,String>
* AWS S3 (Spring Cloud AWS starter S3) + Presigned URL
* Validation(spring-boot-starter-validation)
* Swagger / Springdoc(OpenAPI) — dev 프로파일에서만 활성화
### Frontend
* Thymeleaf(+ Spring Security dialect)
* Bootstrap 5, Bootstrap Icons, 일부 Vanilla JS(fetch)
* TalkJS(채팅)
* Profile 분리: dev, prod (prod에서 Swagger off, ddl-auto validate 등)

## ERD
<img width="700" height="600" alt="ER Diagram" src="https://github.com/user-attachments/assets/cb078d76-ef81-4657-8648-1133835eb4ee" />

## 📌주요 기능
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
* 검색
* 제목 기반 검색(구현은 컨트롤러/서비스 레벨에서 지원)
* 상세 보기
* 이미지 슬라이드/상세 정보
* 채팅 이동, 상태 변경 등 액션 제공
* 작성
* S3 Presigned URL 발급 → 클라이언트가 S3에 직접 업로드 → URL 저장
* 다중 이미지 처리
* 수정
* 기존 이미지 삭제/신규 이미지 업로드 병행
* 삭제 대상 이미지는 서버에 전달하여 정리
* 삭제
* 권한 체크(작성자만)
* S3 이미지 삭제 + 좋아요 정리 + DB 삭제 일괄 처리(공통 메서드로 분리)
### 좋아요 기능
* 좋아요 토글
* 로그인 여부 확인 후 추가/삭제
* 게시글의 likeCnt 동기화
* 프론트에서는 아이콘 상태(채움/비움)와 카운트 즉시 업데이트
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
* 게시글 관리
* 검색/필터/페이지네이션
* 관리자 권한으로 삭제(RESTful DELETE + fetch)
* 리뷰 관리
* 목록/페이지네이션
### 채팅 기능
* TalkJS 연동
* 사용자/게시글 작성자 정보를 기반으로 1:1 대화 생성 또는 inbox 표시
### Redis 기반 기능(성능/UX)
* 조회수
* Redis를 사용해 실시간 집계 후 DB 동기화(스케줄링 기반)
* 최근 본 게시글
* Redis List로 사용자별 최근 본 게시글 관리(중복 제거, 순서 유지)

## 🔒보안/품질
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
