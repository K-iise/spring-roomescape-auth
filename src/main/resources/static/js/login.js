(function () {
  "use strict";

  const form = document.getElementById("login-form");
  const emailInput = document.getElementById("email-input");
  const passwordInput = document.getElementById("password-input");
  const message = document.getElementById("login-message");
  const submitBtn = form.querySelector('button[type="submit"]');

  const REDIRECT_AFTER_LOGIN = "/index.html";

  function showError(text) {
    message.textContent = text;
    message.className = "message message--err";
  }

  function showOk(text) {
    message.textContent = text;
    message.className = "message message--ok";
  }

  async function extractErrorMessage(res) {
    const fallback = "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";
    try {
      const body = await res.json();
      return (body && body.message) || fallback;
    } catch (_) {
      return fallback;
    }
  }

  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();

    const email = emailInput.value.trim();
    const password = passwordInput.value;
    if (!email || !password) {
      showError("이메일과 비밀번호를 모두 입력해 주세요.");
      return;
    }

    submitBtn.disabled = true;
    message.textContent = "";
    message.className = "message";

    try {
      const res = await fetch("/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (res.ok) {
        // JWT를 받아 localStorage에 저장하고, 이후 요청 시 Authorization 헤더로 보낸다.
        const body = await res.json();
        localStorage.setItem("accessToken", body.accessToken);
        showOk("로그인되었습니다. 이동합니다…");
        window.location.assign(REDIRECT_AFTER_LOGIN);
        return;
      }

      showError(await extractErrorMessage(res));
    } catch (_) {
      showError("네트워크 오류로 로그인하지 못했습니다.");
    } finally {
      submitBtn.disabled = false;
    }
  });
})();
