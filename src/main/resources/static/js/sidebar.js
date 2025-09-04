document.addEventListener("DOMContentLoaded", function () {
    const toggleBtn = document.getElementById("sidebarToggle");
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");
    const body = document.body;

    // Check if elements exist
    if (!toggleBtn || !sidebar || !overlay) {
        console.error("Sidebar elements not found");
        return;
    }

    // Toggle sidebar function
    function toggleSidebar() {
        const isOpen = sidebar.classList.contains("show");
        
        if (isOpen) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }

    // Open sidebar
    function openSidebar() {
        sidebar.classList.add("show");
        overlay.classList.add("show");
        body.classList.add("sidebar-open");
    }

    // Close sidebar
    function closeSidebar() {
        sidebar.classList.remove("show");
        overlay.classList.remove("show");
        body.classList.remove("sidebar-open");
    }

    // Toggle button click event
    toggleBtn.addEventListener("click", function(e) {
        e.preventDefault();
        e.stopPropagation();
        toggleSidebar();
    });

    // Overlay click event
    overlay.addEventListener("click", function() {
        closeSidebar();
    });

    // Close sidebar when clicking outside on mobile
    document.addEventListener("click", function(e) {
        if (window.innerWidth <= 768) {
            if (!sidebar.contains(e.target) && 
                !toggleBtn.contains(e.target) && 
                sidebar.classList.contains("show")) {
                closeSidebar();
            }
        }
    });

    // Handle window resize
    window.addEventListener("resize", function() {
        if (window.innerWidth > 768) {
            closeSidebar();
            body.classList.remove("sidebar-open");
        }
    });

    // Prevent sidebar content clicks from closing sidebar
    sidebar.addEventListener("click", function(e) {
        e.stopPropagation();
    });

    // Handle escape key
    document.addEventListener("keydown", function(e) {
        if (e.key === "Escape" && sidebar.classList.contains("show")) {
            closeSidebar();
        }
    });
});
