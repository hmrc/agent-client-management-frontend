$(function() {
    //Accessibility
    var errorSummary =  $('#error-summary-display'),
    $input = $('input:text')
    //Error summary focus
    if (errorSummary){ errorSummary.focus() }
    $input.each( function(){
        if($(this).closest('label').hasClass('form-field--error')){
            $(this).attr('aria-invalid', true)
        }else{
            $(this).attr('aria-invalid', false)
        }
    });
    //Trim inputs and Capitalize postode
    $('[type="submit"]').click(function(){
        $input.each( function(){
            if($(this).val() && $(this).attr('name') === 'postcode'){
                $(this).val($(this).val().toUpperCase().replace(/\s\s+/g, ' ').trim())
            }else{
                $(this).val($(this).val().trim())
            }
        });
    });
    //Add aria-hidden to hidden inputs
    $('[type="hidden"]').attr("aria-hidden", true)


    function nodeListForEach (nodes, callback) {
        if (window.NodeList.prototype.forEach) {
            return nodes.forEach(callback)
        }
        for (var i = 0; i < nodes.length; i++) {
            callback.call(window, nodes[i], i, nodes)
        }
    }

    var $tabs = document.querySelectorAll('[data-module="tabs"]')
    nodeListForEach($tabs, function ($tabs) {
        new window.GOVUKFrontend($tabs).init()
    })

});
