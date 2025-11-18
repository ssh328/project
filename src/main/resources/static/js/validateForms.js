//유효성 검사하도록 하는 부트스트랩 JS코드
// Example starter JavaScript for disabling form submissions if there are invalid fields
(() => {
    'use strict'
  
    // Fetch all the forms we want to apply custom Bootstrap validation styles to
    const form = document.querySelector('.validated-form')
    const saveButton = document.getElementById('saveButton');
  
    // Loop over them and prevent submission
    // Array.from(forms).forEach(form => {
    //    form.addEventListener('submit', event => {
    //    if (!form.checkValidity()) {
    //       event.preventDefault()
    //       event.stopPropagation()
    //    }
    //    form.classList.add('was-validated')
    //    }, false)
    // })

    saveButton.addEventListener('click', event => {
    if (!form.checkValidity()) {
       event.preventDefault()
       event.stopPropagation()
    }
    form.classList.add('was-validated')
    }, false)
     
  })()