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
    
    // Conversation ID 생성 로직
    // 1. postId가 있는 경우 (상품 문의): 상품별 고유 ID 생성
    // 2. postId가 없는 경우 (일반 문의): 기존 방식(oneOnOneId) 사용
    let conversationId;
    if (window.postId) {
        // 상품별 채팅방 ID: post_{postId}_buyer_{me}_seller_{agent}
        conversationId = `post_${window.postId}_buyer_${user.id}_seller_${agent.id}`;
    } else {
        alert('상품 페이지에서 "채팅하기"를 눌러 주세요.');
        window.history.back();
        return;
    }

    const conversation = session.getOrCreateConversation(conversationId);

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