const getAgent = async () => {
  if (!window.postWriterId) return null; // postWriterId가 없으면 null 반환
  const response = await fetch(`getUser?userId=${window.postWriterId}`);
  if (!response.ok) return null; // 요청 실패 시 null
  const data = await response.json();
  return new Talk.User({
    id: data.id,
    name: data.username,
    photoUrl: data.dp,
    email: data.email,
    role: data.role,
  });
};

const getUser = async () => {
  const response = await fetch(`/getUser?userId=${window.userId}`);
  const data = await response.json();
  return new Talk.User({
    id: data.id,
    name: data.username,
    photoUrl: data.dp,
    email: data.email,
    role: data.role,
  });
};

(async function () {
  await Talk.ready;
  const user = await getUser();
  const agent = await getAgent();

  const session = new Talk.Session({
    appId: "t6PBdfX9",
    me: user,
  });

  let inbox;

  if (agent) {
    // 🗣 agent가 존재하는 경우: 1:1 대화 생성
    const conversation = session.getOrCreateConversation(
      Talk.oneOnOneId(user, agent)
    );

    conversation.setAttributes({
      welcomeMessages: [
        "You can start typing your message here and one of our agents will be with you shortly.",
        "Please do not divulge any of your personal information.",
      ]
    });

    conversation.setParticipant(user);
    conversation.setParticipant(agent);

    inbox = session.createInbox(conversation);
    inbox.select(conversation);

  } else {
    // 📬 agent가 없는 경우: 받은 편지함만 표시
    inbox = session.createInbox();
    inbox.select(null);
  }

  inbox.mount(document.getElementById("talkjs-container"));
})();