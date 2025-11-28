const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]').getAttribute('content');

document.getElementById('review_btn').addEventListener('click', function() {
        const content = document.getElementById('reviewContent').value.trim();
        const postId = document.getElementById('postId').value;

        if (content.length === 0) {
            alert("댓글을 입력해주세요.");
            return;
        }

        if (content.length > 100) {
            alert("댓글을 100자 이하으로 작성해주세요.");
            return;
        }

        fetch('/review', {
            method: 'POST',
            headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
            },
            body: JSON.stringify({ content: content, post_id: postId })
        })
        .then(res => res.json())
        .then(data => {
            if (!data.success) {
                alert(data.message);
                return;
            }
            document.getElementById('reviewContent').value = '';

            const reviewList = document.getElementById('reviewList');
            const newReview = document.createElement('div');

            const p = document.createElement('p');
            const usernameEl = document.createElement('b');
            usernameEl.textContent = data.username;
            const contentEl = document.createElement('span');
            contentEl.textContent = data.content;

            p.appendChild(usernameEl);
            p.appendChild(document.createTextNode(": "));
            p.appendChild(contentEl);

            newReview.appendChild(p);
            reviewList.prepend(newReview);
            
            // 최대 5개까지만 쵸시
            while (reviewList.children.length > 5) {
                reviewList.removeChild(reviewList.lastChild);
            }

            const totalPages = Math.ceil(data.totalReviews / 5);
            const pagination = document.getElementById('pagination');
            pagination.innerHTML = '';
            for (let i = 1; i <= totalPages; i++) {
                const a = document.createElement('a');
                a.href = `/detail/${postId}?review=${i}`;
                a.textContent = i + '페이지';
                pagination.appendChild(a);
            }
        })
        .catch(err => console.error("댓글 전송 실패:", err));
    });

