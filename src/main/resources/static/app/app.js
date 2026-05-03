const API_BASE = '/api';
let token = '';
let currentUserId = '';
let currentTargetId = '';
let botId = '';

const app = {
    toggleScreen: (screenId) => {
        document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
        document.getElementById(screenId).classList.add('active');
    },

    register: async () => {
        const login = document.getElementById('reg-username').value;
        const pass = document.getElementById('reg-password').value;
        const role = document.getElementById('reg-role').value;
        if (!login || !pass) return alert('Введите логин и пароль');

        try {
            const res = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ login, password: pass, role })
            });
            const data = await res.json();
            if (data.token) {
                alert('Регистрация успешна! Теперь вы можете войти.');
                app.toggleScreen('login-screen');
                document.getElementById('login-username').value = login;
                document.getElementById('login-password').value = pass;
            } else {
                alert('Ошибка регистрации: Возможно логин уже занят.');
            }
        } catch (e) {
            alert('Ошибка сети.');
        }
    },

    login: async () => {
        const login = document.getElementById('login-username').value;
        const pass = document.getElementById('login-password').value;
        if (!login || !pass) return alert('Введите логин и пароль');

        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ login, password: pass, hardwareTokenCode: '' }) // 2FA optional based on backend
            });
            const data = await res.json();
            if (data.token) {
                token = data.token;
                document.getElementById('login-screen').classList.remove('active');
                document.getElementById('chat-screen').classList.add('active');
                
                document.getElementById('current-user-name').innerText = login;
                document.getElementById('current-user-avatar').innerText = login.charAt(0).toUpperCase();
                
                // Parse JWT to get user role
                try {
                    const payload = JSON.parse(atob(token.split('.')[1]));
                    if (payload.role === 'ADMIN') {
                        document.getElementById('admin-dashboard-btn').style.display = 'block';
                    }
                } catch(e) {}
                
                try {
                    const botRes = await fetch(`${API_BASE}/bot-id`);
                    if(botRes.ok) {
                        const botData = await botRes.json();
                        botId = botData.botId;
                    }
                } catch(e) {}
                
                app.loadUsers();
                setInterval(app.pollMessages, 3000);
            } else {
                alert('Ошибка входа: Возможно, неверный пароль или требуется 2FA. Используйте Dashboard.');
            }
        } catch (e) {
            console.error(e);
            alert('Ошибка сети. Сервер запущен?');
        }
    },

    loadUsers: async () => {
        try {
            const res = await fetch(`${API_BASE}/users`, { headers: { 'Authorization': `Bearer ${token}` } });
            const users = await res.json();
            
            const list = document.getElementById('contact-list');
            list.innerHTML = '';
            
            const myLogin = document.getElementById('current-user-name').innerText;

            users.forEach(u => {
                if (u.login !== myLogin) {
                    const isBot = u.id === botId;
                    const div = document.createElement('div');
                    div.className = `contact-item ${isBot ? 'bot-contact' : ''}`;
                    div.onclick = () => app.selectContact(u.id, u.login, div);
                    div.innerHTML = `
                        <div class="avatar" style="width:40px;height:40px;font-size:1rem; ${isBot ? 'background: linear-gradient(135deg, #10b981, #059669);' : ''}">${isBot ? '🤖' : u.login.charAt(0).toUpperCase()}</div>
                        <div class="user-info">
                            <span class="user-name">${u.login} ${isBot ? '<span class="bot-badge">BOT</span>' : ''}</span>
                        </div>
                    `;
                    list.appendChild(div);
                } else {
                    currentUserId = u.id; // Store my own ID
                }
            });
        } catch (e) { console.error("Error loading users", e); }
    },

    selectContact: (id, name, element) => {
        currentTargetId = id;
        document.querySelectorAll('.contact-item').forEach(el => el.classList.remove('active'));
        if (element) element.classList.add('active');
        
        document.getElementById('active-contact-header').innerText = name;
        document.getElementById('message-input').disabled = false;
        document.getElementById('btn-send').disabled = false;
        document.getElementById('message-list').innerHTML = '<div class="empty-state">Загрузка защищенной истории...</div>';
        
        app.loadHistory(id);
    },

    sendMessage: async () => {
        const input = document.getElementById('message-input');
        const text = input.value.trim();
        if (!text || !currentTargetId) return;

        try {
            // Frontend assumes transparent encryption for UI simplicity, backend handles CryptoService
            const res = await fetch(`${API_BASE}/messages/send`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ senderId: currentUserId, recipientId: currentTargetId, text: text })
            });
            if(res.ok) {
                const msg = await res.json();
                
                // Save sent message locally to bypass decryption limitation
                const sentMsgs = JSON.parse(localStorage.getItem('sentMsgs') || '{}');
                sentMsgs[msg.id] = text;
                localStorage.setItem('sentMsgs', JSON.stringify(sentMsgs));

                input.value = '';
                app.appendMessage(text, 'sent');
            } else {
                alert("Ошибка при шифровании и отправке");
            }
        } catch (e) {
            console.error(e);
        }
    },

    handleKeyPress: (e) => {
        if (e.key === 'Enter') app.sendMessage();
    },

    pollMessages: async () => {
        if(!token) return;
        try {
            const res = await fetch(`${API_BASE}/messages/offline/${currentUserId}`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if(res.ok) {
                const msgs = await res.json();
                if (msgs && msgs.length > 0) {
                    if(currentTargetId) app.loadHistory(currentTargetId);
                }
            }
        } catch (e) {}
    },

    loadHistory: async (otherUserId) => {
        try {
            const res = await fetch(`${API_BASE}/messages/history/${currentUserId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const allMsgs = await res.json();
            
            // Filter messages between me and the other user
            const chatMsgs = allMsgs.filter(m => 
                (m.senderId === otherUserId || m.recipientId === otherUserId)
            ).sort((a,b) => new Date(a.createdAt) - new Date(b.createdAt));
            
            const list = document.getElementById('message-list');
            list.innerHTML = '';
            
            if (chatMsgs.length === 0) {
                list.innerHTML = '<div class="empty-state">Нет сообщений. E2E канал установлен.</div>';
                return;
            }

            for (const m of chatMsgs) {
                let text = "[Зашифровано RSA/AES]";
                
                if (m.senderId === currentUserId) {
                    // It's our own message, load from local storage
                    const sentMsgs = JSON.parse(localStorage.getItem('sentMsgs') || '{}');
                    if (sentMsgs[m.id]) {
                        text = sentMsgs[m.id];
                    }
                } else {
                    // Decrypt incoming message
                    try {
                        const decRes = await fetch(`${API_BASE}/messages/${m.id}/decrypt?recipientId=${currentUserId}`, {
                            method: 'POST',
                            headers: { 'Authorization': `Bearer ${token}` }
                        });
                        if(decRes.ok) {
                            const decData = await decRes.json();
                            text = decData.plaintext;
                        }
                    } catch(e) {}
                }

                const type = m.senderId === currentUserId ? 'sent' : 'received';
                app.appendMessage(text, type);
            }
        } catch (e) { console.error("History error", e); }
    },

    appendMessage: (text, type) => {
        const list = document.getElementById('message-list');
        const empty = list.querySelector('.empty-state');
        if (empty) empty.remove();
        
        const div = document.createElement('div');
        div.className = `msg ${type}`;
        div.innerText = text;
        list.appendChild(div);
        list.scrollTop = list.scrollHeight;
    },
    
    filterContacts: (val) => {
        const items = document.querySelectorAll('.contact-item');
        items.forEach(el => {
            const name = el.querySelector('.user-name').innerText.toLowerCase();
            el.style.display = name.includes(val.toLowerCase()) ? 'flex' : 'none';
        });
    }
};
