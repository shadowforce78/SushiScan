// Gestion du burger menu
function toggleMobileMenu() {
    const burger = document.querySelector('.burger');
    const mobileMenu = document.getElementById('mobileMenu');
    
    burger.classList.toggle('active');
    mobileMenu.classList.toggle('active');
    
    // Empêcher le scroll du body quand le menu est ouvert
    if (mobileMenu.classList.contains('active')) {
        document.body.style.overflow = 'hidden';
    } else {
        document.body.style.overflow = 'auto';
    }
}

function closeMobileMenu() {
    const burger = document.querySelector('.burger');
    const mobileMenu = document.getElementById('mobileMenu');
    
    burger.classList.remove('active');
    mobileMenu.classList.remove('active');
    document.body.style.overflow = 'auto';
}

// Fermer le menu en cliquant en dehors
document.addEventListener('click', function(event) {
    const burger = document.querySelector('.burger');
    const mobileMenu = document.getElementById('mobileMenu');
    const header = document.querySelector('.header');
    
    if (!header.contains(event.target) && mobileMenu.classList.contains('active')) {
        closeMobileMenu();
    }
});

// Fermer le menu lors du redimensionnement vers desktop
window.addEventListener('resize', function() {
    if (window.innerWidth > 768) {
        closeMobileMenu();
    }
});

// Fermer le menu avec la touche Escape
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeMobileMenu();
    }
});
