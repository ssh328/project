/**
 * 게시글 작성자 정보를 조회하여 TalkJS User 객체로 변환
 * @returns {Promise<Talk.User|null>} TalkJS User 객체 또는 null (작성자 정보가 없는 경우)
 */
const getAgent = async () => {
  if (!window.postWriterId) return null; // postWriterId가 없으면 null 반환
  const response = await fetch(`/getUser?userId=${window.postWriterId}`);
  if (!response.ok) return null; // 요청 실패 시 null
  const data = await response.json();
  // 서버 응답 데이터를 TalkJS User 객체로 변환
  return new Talk.User({
    id: data.id,
    name: data.username,
    photoUrl: data.dp,
    email: data.email,
    role: data.role,
  });
};

/**
 * 현재 로그인한 사용자 정보를 조회하여 TalkJS User 객체로 변환
 * @returns {Promise<Talk.User>} 현재 사용자의 TalkJS User 객체
 */
const getUser = async () => {
  const response = await fetch(`/getUser?userId=${window.userId}`);
  const data = await response.json();
  // 서버 응답 데이터를 TalkJS User 객체로 변환
  return new Talk.User({
    id: data.id,
    name: data.username,
    photoUrl: data.dp,
    email: data.email,
    role: data.role,
  });
};

/**
 * TalkJS 채팅 초기화
 * 페이지 로드 시 실행되어 채팅 세션을 생성하고 UI를 마운트
 * 게시글 작성자가 있는 경우: 1:1 대화 생성
 * 게시글 작성자가 없는 경우: 전체 받은 편지함 표시
 */
(async function () {
  // TalkJS 라이브러리 로드 대기
  await Talk.ready;
  
  // 현재 사용자와 게시글 작성자 정보 조회
  const user = await getUser();
  const agent = await getAgent();

  // TalkJS 세션 생성
  const session = new Talk.Session({
    appId: window.talkjsAppId,
    me: user,
  });

  let inbox;

  if (agent) {
    // agent가 존재하는 경우: 1:1 대화 생성
    // 사용자와 게시글 작성자 간의 고유 대화 ID 생성
    const conversation = session.getOrCreateConversation(
      Talk.oneOnOneId(user, agent)
    );

    // 대화 시작 시 표시할 환영 메시지 설정
    conversation.setAttributes({
      welcomeMessages: [
        "You can start typing your message here and one of our agents will be with you shortly.",
        "Please do not divulge any of your personal information.",
      ]
    });

    // 대화 참여자 설정
    conversation.setParticipant(user);
    conversation.setParticipant(agent);

    // 특정 대화를 표시하는 받은 편지함 생성 및 선택
    inbox = session.createInbox(conversation);
    inbox.select(conversation);

  } else {
    // agent가 없는 경우: 전체 받은 편지함만 표시
    inbox = session.createInbox();
    inbox.select(null);
  }

  // 채팅 UI를 DOM에 마운트
  inbox.mount(document.getElementById("talkjs-container"));
})();