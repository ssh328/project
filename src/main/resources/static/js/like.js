const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]').getAttribute('content');

document.querySelectorAll(".increase-cnt-btn").forEach(heart_btn => {
            heart_btn.addEventListener('click', function() {
                const postId = Number(this.dataset.postId);
                const heartIcon = this.querySelector('i');

                fetch(`/like/${postId}`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        [csrfHeader]: csrfToken
                    }
                })
                .then(res => res.json())
                .then(data => {
                    // message를 담아 보냈을 때만 로그인 실패로 인식
                    if (!data.loggedIn) {
                        console.log("에러 발생")
                        alert(data.message);
                        window.location.href = "/login";
                        return;
                    }

                    if (heartIcon.classList.contains('fa-solid')) {
                        heartIcon.classList.remove('fa-solid', 'fa-bounce');
                        heartIcon.classList.add('fa-regular');
                        heartIcon.style.color = '#000000';
                        heartIcon.style.removeProperty('--fa-animation-iteration-count');
                    } else {
                        heartIcon.classList.remove('fa-regular');
                        heartIcon.classList.add('fa-solid', 'fa-bounce');
                        heartIcon.style.color = '#ff0000';
                        heartIcon.style.setProperty('--fa-animation-iteration-count', '1');
                    }

                    this.querySelector(".like-count").textContent = data.likeCount;
                })
                .catch(err => console.error("좋아요 요청 실패:", err));
            });
        });