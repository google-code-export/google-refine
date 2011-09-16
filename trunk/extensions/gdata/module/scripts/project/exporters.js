/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

CustomTabularExporterDialog.uploadTargets.push({
  id: 'gdata/google-spreadsheet',
  label: 'A new Google spreadsheet',
  handler: function(options, exportAllRows, onDone) {
    var doUpload = function() {
      var name = window.prompt('Enter name of new spreadsheet', theProject.metadata.name);
      if (name) {
        var dismiss = DialogSystem.showBusy('Uploading...');
        $.post(
          "/command/gdata/upload",
          {
            "project" : theProject.id,
            "engine" : exportAllRows ? '' : JSON.stringify(ui.browsingEngine.getJSON()),
            "name" : name,
            "format" : options.format,
            "options" : JSON.stringify(options)
          },
          function(o) {
            dismiss();
            
            if (o.url) {
              window.open(o.url, '_blank');
            }
            onDone();
          },
          "json"
        );
      }
    };
    
    var messageListener = function(evt) {
      window.removeEventListener("message", messageListener, false);
      if ($.cookie('authsub_token')) {
        doUpload();
      }
    };
    
    var authenticate = function() {
      window.addEventListener("message", messageListener, false);
      window.open(
        "/command/gdata/authorize",
        "google-refine-gdata-signin",
        "resizable=1,width=600,height=450"
      );
    };
    
    authenticate();
  }
});
