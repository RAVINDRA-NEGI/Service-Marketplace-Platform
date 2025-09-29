// Messaging Utility Functions
class MessagingUtils {
    static scrollToBottom(element) {
        if (element) {
            element.scrollTop = element.scrollHeight;
        }
    }

    static formatDate(date) {
        const now = new Date();
        const messageDate = new Date(date);
        
        if (messageDate.toDateString() === now.toDateString()) {
            return messageDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } else if (messageDate.toDateString() === new Date(now - 86400000).toDateString()) {
            return 'Yesterday';
        } else {
            return messageDate.toLocaleDateString();
        }
    }

    static formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    static getFileIcon(fileType) {
        const iconMap = {
            'photo': 'fas fa-image',
            'document': 'fas fa-file-pdf',
            'video': 'fas fa-video',
            'audio': 'fas fa-music',
            'other': 'fas fa-file'
        };
        return iconMap[fileType] || 'fas fa-file';
    }

    static validateFileType(file) {
        const validTypes = [
            'image/jpeg', 'image/png', 'image/gif', 'image/webp',
            'application/pdf', 'text/plain',
            'application/msword', 'application/vnd.ms-word',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        ];
        
        return validTypes.includes(file.type);
    }

    static validateFileSize(file, maxSize = 10 * 1024 * 1024) { // 10MB default
        return file.size <= maxSize;
    }

    static createFilePreview(file) {
        const preview = document.createElement('div');
        preview.className = 'file-preview d-flex align-items-center p-2 border rounded';

        if (file.type.startsWith('image/')) {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(file);
            img.className = 'me-2';
            img.style.maxWidth = '100px';
            img.style.maxHeight = '100px';
            preview.appendChild(img);
        } else {
            const icon = document.createElement('i');
            icon.className = 'fas fa-file me-2';
            icon.style.fontSize = '2rem';
            preview.appendChild(icon);
        }

        const fileInfo = document.createElement('div');
        fileInfo.className = 'file-info';
        fileInfo.innerHTML = `
            <div class="fw-bold">${file.name}</div>
            <small class="text-muted">${this.formatFileSize(file.size)}</small>
        `;
        preview.appendChild(fileInfo);

        return preview;
    }

    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    static showQuickReplies() {
        const quickReplies = document.getElementById('quick-replies');
        if (quickReplies) {
            quickReplies.style.display = 'block';
        }
    }

    static hideQuickReplies() {
        const quickReplies = document.getElementById('quick-replies');
        if (quickReplies) {
            quickReplies.style.display = 'none';
        }
    }

    static setupEmojiPicker() {
        const emojiPicker = document.getElementById('emoji-picker');
        if (emojiPicker) {
            emojiPicker.addEventListener('click', () => {
                // Simple emoji picker - you can enhance this with a proper emoji picker library
                const emojis = ['😊', '👍', '❤️', '😂', '🎉', '🔥', '✨', '💯'];
                const randomEmoji = emojis[Math.floor(Math.random() * emojis.length)];
                
                const messageInput = document.getElementById('message-input');
                if (messageInput) {
                    messageInput.value += randomEmoji;
                    messageInput.focus();
                }
            });
        }
    }

    static setupSearchFunctionality() {
        const searchInput = document.getElementById('professional-search');
        if (searchInput) {
            const debouncedSearch = this.debounce((searchTerm) => {
                this.searchProfessionals(searchTerm);
            }, 300);

            searchInput.addEventListener('input', (e) => {
                const searchTerm = e.target.value.trim();
                if (searchTerm.length > 2) {
                    debouncedSearch(searchTerm);
                } else {
                    this.clearSearchResults();
                }
            });
        }
    }

    static searchProfessionals(searchTerm) {
        // This would make an API call to search for professionals
        // For now, we'll just show a mock implementation
        fetch(`/api/professionals/search?query=${encodeURIComponent(searchTerm)}`)
            .then(response => response.json())
            .then(data => {
                this.displaySearchResults(data.professionals);
            })
            .catch(error => {
                console.error('Search error:', error);
            });
    }

    static displaySearchResults(professionals) {
        const resultsContainer = document.getElementById('search-results');
        if (!resultsContainer) return;

        resultsContainer.innerHTML = '';

        if (professionals.length === 0) {
            resultsContainer.innerHTML = '<div class="list-group-item">No professionals found</div>';
            return;
        }

        professionals.forEach(professional => {
            const item = document.createElement('a');
            item.className = 'list-group-item list-group-item-action';
            item.href = '#';
            item.innerHTML = `
                <div class="d-flex align-items-center">
                    <img src="${professional.avatarUrl || '/images/default-avatar.png'}" 
                         class="rounded-circle me-3" 
                         width="40" height="40" 
                         alt="Professional Avatar">
                    <div>
                        <div class="fw-bold">${professional.name}</div>
                        <small class="text-muted">${professional.category}</small>
                    </div>
                </div>
            `;
            item.addEventListener('click', (e) => {
                e.preventDefault();
                this.startConversationWithProfessional(professional.id);
            });

            resultsContainer.appendChild(item);
        });
    }

    static clearSearchResults() {
        const resultsContainer = document.getElementById('search-results');
        if (resultsContainer) {
            resultsContainer.innerHTML = '';
        }
    }

    static startConversationWithProfessional(professionalId) {
        // This would create a conversation and redirect to the chat page
        fetch('/api/conversations/start', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                professionalId: professionalId
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                window.location.href = `/chat/conversation/${data.conversationId}`;
            } else {
                alert('Failed to start conversation: ' + data.error);
            }
        })
        .catch(error => {
            console.error('Error starting conversation:', error);
            alert('Error starting conversation');
        });
    }

    static setupFileUpload() {
        const fileUploadBtn = document.getElementById('file-upload-btn');
        const fileInput = document.getElementById('file-upload');

        if (fileUploadBtn && fileInput) {
            fileUploadBtn.addEventListener('click', () => {
                fileInput.click();
            });

            fileInput.addEventListener('change', (e) => {
                const file = e.target.files[0];
                if (file) {
                    if (!this.validateFileType(file)) {
                        alert('Invalid file type. Please upload an image or document.');
                        e.target.value = '';
                        return;
                    }

                    if (!this.validateFileSize(file)) {
                        alert('File too large. Maximum size is 10MB.');
                        e.target.value = '';
                        return;
                    }

                    // Show preview
                    const previewContainer = document.getElementById('file-preview');
                    if (previewContainer) {
                        previewContainer.innerHTML = '';
                        previewContainer.appendChild(this.createFilePreview(file));
                        previewContainer.style.display = 'block';
                    }
                }
            });
        }
    }
}

// Initialize utilities when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    MessagingUtils.setupEmojiPicker();
    MessagingUtils.setupSearchFunctionality();
    MessagingUtils.setupFileUpload();
    
    // Auto-scroll to bottom of chat messages
    const messagesContainer = document.getElementById('chat-messages');
    if (messagesContainer) {
        MessagingUtils.scrollToBottom(messagesContainer);
    }
});

// Export for global use
window.MessagingUtils = MessagingUtils;