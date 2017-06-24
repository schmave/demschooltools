// Original JavaScript code by Chirp Internet: www.chirp.com.au
// Please acknowledge use of this code by including this header.

var init = function() {
  var modalWrapper = document.getElementById("modal_wrapper");
  var modalWindow  = document.getElementById("modal_window");

  var openModal = function(e)
  {
    modalWrapper.className = "overlay";
    var overflow = modalWindow.offsetHeight - document.documentElement.clientHeight;
    if(overflow > 0) {
      modalWindow.style.maxHeight = (parseInt(window.getComputedStyle(modalWindow).height) - overflow) + "px";
    }
    modalWindow.style.marginTop = (-modalWindow.offsetHeight)/2 + "px";
    modalWindow.style.marginLeft = (-modalWindow.offsetWidth)/2 + "px";
    e.preventDefault ? e.preventDefault() : e.returnValue = false;
    if (e.stopPropagation) e.stopPropagation();

    var textArea = $('#modal_feedback textarea');
    textArea.val(textArea.val() + '\n\n---------------------\nFeedback sent from: ' + window.location);
  };

  var closeModal = function(e)
  {
    modalWrapper.className = "";
    e.preventDefault ? e.preventDefault() : e.returnValue = false;
    return false;
  };

  var clickHandler = function(e) {
    if(!e.target) e.target = e.srcElement;
    if(e.target.tagName == "DIV") {
      if(e.target.id != "modal_window") closeModal(e);
    }
  };

  var keyHandler = function(e) {
    if(e.keyCode == 27) closeModal(e);
  };

  $('#modal_feedback').ajaxForm({"success" : function(response, status) {
        $('#modal_feedback').append('<p>Your message has been sent. Thank you!</p>');
       }});

  if(document.addEventListener) {
    document.getElementById("modal_open").addEventListener("click", openModal, false);
    document.getElementById("modal_close").addEventListener("click", closeModal, false);
    document.addEventListener("click", clickHandler, false);
    document.addEventListener("keydown", keyHandler, false);
  } else {
    document.getElementById("modal_open").attachEvent("onclick", openModal);
    document.getElementById("modal_close").attachEvent("onclick", closeModal);
    document.attachEvent("onclick", clickHandler);
    document.attachEvent("onkeydown", keyHandler);
  }
};

module.exports = {
    init: init,
};
