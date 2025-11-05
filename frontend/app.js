const form = document.getElementById('matching-form');
const resultSection = document.getElementById('matching-result');
const resultContent = document.getElementById('matching-result-content');

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = new FormData(form);
  const payload = Object.fromEntries(formData.entries());

  resultSection.classList.remove('hidden');
  resultContent.textContent = '正在提交匹配请求...';

  try {
    const response = await fetch('/api/match-enterprise', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(`服务器返回错误状态：${response.status}`);
    }

    const result = await response.json();
    resultContent.textContent = JSON.stringify(result, null, 2);
  } catch (error) {
    resultContent.textContent = `提交失败：${error.message}`;
  }
});
