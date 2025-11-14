// Custom Swagger UI script to hide Example Value and Schema tabs
(function() {
    'use strict';
    
    // Function to hide Example Value and Schema tabs
    function hideExampleAndSchemaTabs() {
        // Find all response sections
        const responseSections = document.querySelectorAll('.response, .opblock-body');
        
        responseSections.forEach(function(section) {
            // Find all tabs
            const tabs = section.querySelectorAll('.tab-item, .response-content-type .tab-item');
            
            tabs.forEach(function(tab) {
                const tabText = tab.textContent.trim().toLowerCase();
                // Hide Example Value and Schema tabs
                if (tabText.includes('example value') || tabText.includes('schema')) {
                    tab.style.display = 'none';
                }
            });
            
            // Also hide by data attributes or classes
            const exampleTabs = section.querySelectorAll('[data-value="example"], [data-value="schema"]');
            exampleTabs.forEach(function(tab) {
                tab.style.display = 'none';
            });
        });
    }
    
    // Function to run when Swagger UI is ready
    function init() {
        hideExampleAndSchemaTabs();
        
        // Use MutationObserver to catch dynamically loaded content
        const observer = new MutationObserver(function(mutations) {
            hideExampleAndSchemaTabs();
        });
        
        // Start observing
        if (document.body) {
            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
        }
    }
    
    // Run when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
    // Also run after delays to catch dynamically loaded content
    setTimeout(hideExampleAndSchemaTabs, 500);
    setTimeout(hideExampleAndSchemaTabs, 1000);
    setTimeout(hideExampleAndSchemaTabs, 2000);
    setTimeout(hideExampleAndSchemaTabs, 3000);
})();

