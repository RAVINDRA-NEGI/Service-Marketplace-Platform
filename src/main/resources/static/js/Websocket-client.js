// WebSocket Client for Real-time Messaging
class WebSocketClient {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.currentConversationId = null;
        this.typingTimer = null;
        this.isTyping = false;
        
        this.initialize();
    }

    initialize() {
        this.connect();
        this.setupEventListeners();
        this.setupTypingIndicator();
    }

    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            this.connected = true;
            
            // Subscribe to user-specific topics
            this.subscribeToUserTopics();
        }, (error) => {
            console.error('WebSocket connection error:', error);
            this.connected = false;
            
            // Retry connection after 5 seconds
            setTimeout(() => this.connect(), 5000);
        });
    }

    subscribeToUserTopics() {
        // Subscribe to error topic
        this.stompClient.subscribe('/user/queue/errors', (message) => {
            const errorData = JSON.parse(message.body);
            this.showError(errorData.error);
        });

        // Subscribe to notifications
        this.stompClient.subscribe('/user/queue/notifications', (message) => {
            const notification = JSON.parse(message.body);
            this.showNotification(notification);
        });
    }

    subscribeToConversation(conversationId) {
        if (!this.stompClient || !this.connected) return;

        // Unsubscribe from previous conversation
        if (this.currentConversationId) {
            this.stompClient.unsubscribe(`/topic/conversation/${this.currentConversationId}`);
            this.stompClient.unsubscribe(`/user/queue/conversation/${this.currentConversationId}/notification`);
        }

        // Subscribe to current conversation
        this.stompClient.subscribe(`/topic/conversation/${conversationId}`, (message) => {
            const chatMessage = JSON.parse(message.body);
            this.displayMessage(chatMessage);
        });

        this.stompClient.subscribe(`/user/queue/conversation/${conversationId}/notification`, (message) => {
            const chatMessage = JSON.parse(message.body);
            this.displayNotification(chatMessage);
        });

        // Subscribe to typing indicators
        this.stompClient.subscribe(`/topic/conversation/${conversationId}/typing`, (message) => {
            const typingData = JSON.parse(message.body);
            this.displayTypingIndicator(typingData);
        });

        // Subscribe to read receipts
        this.stompClient.subscribe(`/user/queue/conversation/${conversationId}/read-receipt`, (message) => {
            const readReceipt = JSON.parse(message.body);
            this.updateMessageStatus(readReceipt);
        });

        this.currentConversationId = conversationId;
    }

    sendMessage(conversationId, content, messageType = 'TEXT', file = null) {
        if (!this.stompClient || !this.connected) {
            this.showError('Not connected to WebSocket');
            return;
        }

        const message = {
            conversationId: conversationId,
            content: content,
            messageType: messageType,
            timestamp: new Date().toISOString()
        };

        this.stompClient.send(`/app/chat/send/${conversationId}`, {}, JSON.stringify(message));
    }

    sendTypingIndicator(conversationId) {
        if (!this.stompClient || !this.connected) return;

        this.stompClient.send(`/app/chat/typing/${conversationId}`, {}, JSON.stringify({}));
    }

    markAsRead(conversationId) {
        if (!this.stompClient || !this.connected) return;

        this.stompClient.send(`/app/chat/mark-as-read/${conversationId}`, {}, JSON.stringify({}));
    }

    setupEventListeners() {
        // Send message on form submit
        document.getElementById('message-form')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleSendMessage();
        });

        // Quick reply buttons
        document.querySelectorAll('.quick-reply').forEach(button => {
            button.addEventListener('click', (e) => {
                const reply = e.target.getAttribute('data-reply');
                this.insertQuickReply(reply);
            });
        });

        // File upload button
        document.getElementById('file-upload-btn')?.addEventListener('click', () => {
            document.getElementById('file-upload').click();
        });

        // File input change
        document.getElementById('file-upload')?.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                this.handleFileUpload(file);
            }
        });

        // Mark as read when opening conversation
        if (this.currentConversationId) {
            this.markAsRead(this.currentConversationId);
        }
    }

    setupTypingIndicator() {
        const messageInput = document.getElementById('message-input');
        if (!messageInput) return;

        messageInput.addEventListener('keypress', () => {
            if (!this.isTyping) {
                this.isTyping = true;
                this.sendTypingIndicator(this.currentConversationId);
            }

            clearTimeout(this.typingTimer);
            this.typingTimer = setTimeout(() => {
                this.isTyping = false;
            }, 1000);
        });
    }

    handleSendMessage() {
        const messageInput = document.getElementById('message-input');
        const content = messageInput.value.trim();

        if (!content || !this.currentConversationId) return;

        this.sendMessage(this.currentConversationId, content, 'TEXT');
        messageInput.value = '';
        
        // Hide quick replies after sending
        document.getElementById('quick-replies').style.display = 'none';
    }

    insertQuickReply(reply) {
        const messageInput = document.getElementById('message-input');
        messageInput.value = reply;
        messageInput.focus();
    }

    handleFileUpload(file) {
        if (!this.validateFile(file)) {
            this.showError('Invalid file type or size');
            return;
        }

        // Show loading state
        this.showLoading();

        // Upload file via REST API first
        const formData = new FormData();
        formData.append('file', file);
        formData.append('conversationId', this.currentConversationId);

        fetch('/api/files/upload', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // Send message with file reference
                this.sendMessage(this.currentConversationId, data.originalFilename, 'PHOTO');
            } else {
                this.showError(data.error);
            }
        })
        .catch(error => {
            this.showError('File upload failed: ' + error.message);
        })
        .finally(() => {
            this.hideLoading();
        });
    }

    validateFile(file) {
        const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf'];
        const maxSize = 10 * 1024 * 1024; // 10MB

        return validTypes.includes(file.type) && file.size <= maxSize;
    }

    displayMessage(message) {
        const messagesContainer = document.getElementById('chat-messages');
        if (!messagesContainer) return;

        const messageElement = this.createMessageElement(message);
        messagesContainer.appendChild(messageElement);

        // Scroll to bottom
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    createMessageElement(message) {
        const div = document.createElement('div');
        div.className = message.isFromCurrentUser ? 'message-bubble message-sent' : 'message-bubble message-received';

        let contentHtml = '';

        // Message content based on type
        if (message.messageType === 'TEXT') {
            contentHtml = `<div class="message-text">${this.escapeHtml(message.content)}</div>`;
        } else if (message.messageType === 'PHOTO' && message.files && message.files.length > 0) {
            contentHtml = '<div class="message-photo">';
            message.files.forEach(file => {
                contentHtml += `<img src="${file.fileUrl}" class="img-fluid rounded" alt="Photo attachment" style="max-width: 200px; max-height: 200px;">`;
            });
            contentHtml += '</div>';
        } else if (message.messageType === 'DOCUMENT' && message.files && message.files.length > 0) {
            contentHtml = '<div class="message-document">';
            message.files.forEach(file => {
                contentHtml += `
                    <a href="${file.fileUrl}" class="d-flex align-items-center text-decoration-none" target="_blank">
                        <i class="fas fa-file-${file.fileType === 'photo' ? 'image' : 'pdf'} fa-2x text-danger me-2"></i>
                        <div>
                            <div>${this.escapeHtml(file.originalFilename)}</div>
                            <small class="text-muted">${this.formatFileSize(file.fileSize)}</small>
                        </div>
                    </a>
                `;
            });
            contentHtml += '</div>';
        }

        // Message metadata
        const timeString = new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const statusIcon = message.isFromCurrentUser ? 
            `<span class="${message.messageStatus === 'READ' ? 'text-primary' : 'text-muted'}">
                <i class="${message.messageStatus === 'READ' ? 'fas fa-check-double' : 'fas fa-check'}"></i>
            </span>` : '';

        div.innerHTML = `
            <div class="message-content">
                ${contentHtml}
                <div class="message-meta">
                    <small class="text-muted">${timeString}</small>
                    ${statusIcon}
                </div>
            </div>
        `;

        return div;
    }

    displayTypingIndicator(typingData) {
        // Show typing indicator in chat header or message area
        const typingIndicator = document.createElement('div');
        typingIndicator.className = 'typing-indicator text-center py-2';
        typingIndicator.innerHTML = `
            <small class="text-muted">
                <i class="fas fa-ellipsis-h"></i>
                ${typingData.typingUser} is typing...
            </small>
        `;

        // Add to chat messages container
        const messagesContainer = document.getElementById('chat-messages');
        messagesContainer.appendChild(typingIndicator);

        // Remove after 3 seconds
        setTimeout(() => {
            typingIndicator.remove();
        }, 3000);
    }

    updateMessageStatus(readReceipt) {
        // Update message status in UI when read receipt is received
        // This would involve finding messages and updating their status icons
        console.log('Message read by user:', readReceipt.readByUserId);
    }

    displayNotification(message) {
        // Show notification for new messages
        this.showNotification(`New message from ${message.senderName}`);
    }

    showNotification(message) {
        // Create a toast notification
        const toast = document.createElement('div');
        toast.className = 'toast show position-fixed top-0 end-0 m-3';
        toast.style.zIndex = '9999';
        toast.innerHTML = `
            <div class="toast-header">
                <strong class="me-auto">New Message</strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${this.escapeHtml(message)}
            </div>
        `;

        document.body.appendChild(toast);

        // Remove after 5 seconds
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 5000);
    }

    showError(message) {
        // Show error message in UI
        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3';
        errorDiv.style.zIndex = '9999';
        errorDiv.innerHTML = `
            ${this.escapeHtml(message)}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(errorDiv);

        // Remove after 5 seconds
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.remove();
            }
        }, 5000);
    }

    showLoading() {
        const sendBtn = document.getElementById('send-btn');
        if (sendBtn) {
            sendBtn.innerHTML = '<span class="loading-spinner"></span>';
            sendBtn.disabled = true;
        }
    }

    hideLoading() {
        const sendBtn = document.getElementById('send-btn');
        if (sendBtn) {
            sendBtn.innerHTML = '<i class="fas fa-paper-plane"></i>';
            sendBtn.disabled = false;
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.connected = false;
        }
    }
}

// Initialize WebSocket client when DOM is loaded
let webSocketClient;

function initializeChat() {
    webSocketClient = new WebSocketClient();
    
    // Subscribe to current conversation if we're on a conversation page
    const conversationId = getConversationIdFromUrl();
    if (conversationId) {
        webSocketClient.subscribeToConversation(conversationId);
        
        // Mark as read when page loads
        setTimeout(() => {
            webSocketClient.markAsRead(conversationId);
        }, 1000);
    }
}

function getConversationIdFromUrl() {
    const path = window.location.pathname;
    const match = path.match(/\/chat\/conversation\/(\d+)/);
    return match ? parseInt(match[1]) : null;
}

// Export for global use
window.WebSocketClient = WebSocketClient;
window.initializeChat = initializeChat;