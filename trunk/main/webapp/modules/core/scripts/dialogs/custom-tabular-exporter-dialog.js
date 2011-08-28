/*

Copyright 2010, Google Inc.
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

function CustomTabularExporterDialog(options) {
  options = options || {
    format: 'tsv',
    lineSeparator: '\n',
    separator: '\t',
    encoding: 'UTF-8',
    outputColumnHeaders: true,
    outputBlankRows: false,
    xlsx: false,
    columns: null
  };
  
  this._columnOptionMap = {};
  this._createDialog(options);
};

CustomTabularExporterDialog.prototype._createDialog = function(options) {
  var self = this;
  
  this._dialog = $(DOM.loadHTML("core", "scripts/dialogs/custom-tabular-exporter-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._level = DialogSystem.showDialog(this._dialog);
  
  $("#custom-tabular-exporter-tabs-format").css("display", "");
  $("#custom-tabular-exporter-tabs-content").css("display", "");
  $("#custom-tabular-exporter-tabs-code").css("display", "");
  $("#custom-tabular-exporter-tabs").tabs();
  
  /*
   * Populate column list.
   */
  for (var i = 0; i < theProject.columnModel.columns.length; i++) {
    var column = theProject.columnModel.columns[i];
    var name = column.name;
    var div = $('<div>')
        .addClass("custom-tabular-exporter-dialog-column")
        .attr("column", name)
        .appendTo(this._elmts.columnList);
    
    $('<input>')
      .attr('type', 'checkbox')
      .attr('checked', 'checked')
      .appendTo(div);
    $('<span>')
      .text(name)
      .appendTo(div);
    
    this._columnOptionMap[name] = {
      name: name,
      reconSettings: {
        output: 'entity-name',
        blankUnmatchedCells: false,
        linkToEntityPages: true
      },
      dateSettings: {
        format: 'iso-8601',
        custom: '',
        useLocalTimeZone: false,
        omitTime: false
      }
    };
  }
  this._elmts.columnList.sortable({});
  
  /*
   * Hook up event handlers.
   */
  this._elmts.encodingInput.click(function(evt) {
    Encoding.selectEncoding($(this), function() {
      self._updateOptionCode();
    });
  });
  
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').click(function() {
    self._elmts.columnList.find('.custom-tabular-exporter-dialog-column').removeClass('selected');
    $(this).addClass('selected');
    self._selectColumn(this.getAttribute('column'));
    self._updateOptionCode();
  });
  this._elmts.selectAllButton.click(function() {
    self._elmts.columnList.find('input[type="checkbox"]').attr('checked', 'checked');
    self._updateOptionCode();
  });
  this._elmts.deselectAllButton.click(function() {
    self._elmts.columnList.find('input[type="checkbox"]').attr('checked', '');
    self._updateOptionCode();
  });
  
  this._elmts.columnOptionPane.find('input').bind('change', function() {
    self._updateCurrentColumnOptions();
  });
  $('#custom-tabular-exporter-tabs-content').find('input').bind('change', function() {
    self._updateOptionCode();
  });
  $('#custom-tabular-exporter-tabs-format').find('input').bind('change', function() {
    self._updateOptionCode();
  });
  
  this._elmts.applyOptionCodeButton.click(function(evt) { self._applyOptionCode(); });
  this._elmts.cancelButton.click(function() { self._dismiss(); });
  this._elmts.exportButton.click(function() { self._commit(); });
  this._elmts.previewButton.click(function(evt) { self._preview(); });
  
  this._configureUIFromOptionCode(options);
  this._updateOptionCode();
};

CustomTabularExporterDialog.prototype._configureUIFromOptionCode = function(options) {
  this._dialog.find('input[name="custom-tabular-exporter-format"][value="' + options.format + '"]').attr('checked', 'checked');
  this._elmts.separatorInput[0].value = String.encodeSeparator(options.separator || ',');
  this._elmts.lineSeparatorInput[0].value = String.encodeSeparator(options.lineSeparator || '\n');
  this._elmts.encodingInput[0].value = options.encoding;
  this._elmts.outputColumnHeadersCheckbox.attr('checked', (options.outputColumnHeaders) ? 'checked' : '');
  this._elmts.outputBlankRowsCheckbox.attr('checked', (options.outputBlankRows) ? 'checked' : '');
  this._elmts.xlsxCheckbox.attr('checked', (options.xlsx) ? 'checked' : '');
  
  if (options.columns != null) {
    var self = this;
    this._elmts.columnList.find('.custom-tabular-exporter-dialog-column input[type="checkbox"]').attr('checked', '');
    $.each(options.columns, function() {
      var name = this.name;
      self._columnOptionMap[name] = this;
      self._elmts.columnList.find('.custom-tabular-exporter-dialog-column').each(function() {
        if (this.getAttribute('column') == name) {
          $(this).find('input[type="checkbox"]').attr('checked', 'checked');
        }
      });
    });
  }
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').first().addClass('selected');
  this._selectColumn(theProject.columnModel.columns[0].name);
};

CustomTabularExporterDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

CustomTabularExporterDialog.prototype._preview = function() {
  var options = this._getOptionCode();
  console.log(options);
};

CustomTabularExporterDialog.prototype._selectColumn = function(columnName) {
  this._elmts.columnNameSpan.text(columnName);
  
  var columnOptions = this._columnOptionMap[columnName];
  
  this._elmts.columnOptionPane.find('input[name="custom-tabular-exporter-recon"][value="' +
    columnOptions.reconSettings.output + '"]').attr('checked', 'checked');
  this._elmts.reconBlankUnmatchedCheckbox.attr('checked', columnOptions.reconSettings.blankUnmatchedCells ? 'checked' : '');
  this._elmts.reconLinkCheckbox.attr('checked', columnOptions.reconSettings.linkToEntityPages ? 'checked' : '');
  
  this._elmts.columnOptionPane.find('input[name="custom-tabular-exporter-date"][value="' +
    columnOptions.dateSettings.format + '"]').attr('checked', 'checked');
  this._elmts.dateCustomInput.val(columnOptions.dateSettings.custom);
  this._elmts.dateLocalTimeZoneCheckbox.attr('checked', columnOptions.dateSettings.useLocalTimeZone ? 'checked' : '');
  this._elmts.omitTimeCheckbox.attr('checked', columnOptions.dateSettings.omitTime ? 'checked' : '');
};

CustomTabularExporterDialog.prototype._updateCurrentColumnOptions = function() {
  var selectedColumnName = this._elmts.columnList
    .find('.custom-tabular-exporter-dialog-column.selected').attr('column');
  
  var columnOptions = this._columnOptionMap[selectedColumnName];
  
  columnOptions.reconSettings.output = this._elmts.columnOptionPane
    .find('input[name="custom-tabular-exporter-recon"]:checked').val();
  columnOptions.reconSettings.blankUnmatchedCells = this._elmts.reconBlankUnmatchedCheckbox[0].checked;
  columnOptions.reconSettings.linkToEntityPages = this._elmts.reconLinkCheckbox[0].checked;
  
  columnOptions.dateSettings.format = this._elmts.columnOptionPane
    .find('input[name="custom-tabular-exporter-date"]:checked').val();
  columnOptions.dateSettings.custom = this._elmts.dateCustomInput.val();
  columnOptions.dateSettings.useLocalTimeZone = this._elmts.dateLocalTimeZoneCheckbox[0].checked;
  columnOptions.dateSettings.omitTime = this._elmts.omitTimeCheckbox[0].checked;
};

CustomTabularExporterDialog.prototype._updateOptionCode = function() {
  this._elmts.optionCodeInput.val(JSON.stringify(this._getOptionCode(), null, 2));
};

CustomTabularExporterDialog.prototype._applyOptionCode = function() {
  var s = this._elmts.optionCodeInput.val();
  try {
    var json = JSON.parse(s);
    this._configureUIFromOptionCode(json);
    
    alert('Option code successfully applied.');
  } catch (e) {
    alert('Error applying option code: ' + e);
  }
};

CustomTabularExporterDialog.prototype._getOptionCode = function() {
  var options = {
    format: this._dialog.find('input[name="custom-tabular-exporter-format"]:checked').val()
  };
  
  if (options.format == 'excel') {
    options.xlsx = this._elmts.xlsxCheckbox[0].checked;
  } else if (options.format != 'html') {
    options.separator = String.decodeSeparator(this._elmts.separatorInput.val());
    options.lineSeparator = String.decodeSeparator(this._elmts.lineSeparatorInput.val());
    options.encoding = this._elmts.encodingInput.val();
  }
  options.outputColumnHeaders = this._elmts.outputColumnHeadersCheckbox[0].checked;
  options.outputBlankRows = this._elmts.outputBlankRowsCheckbox[0].checked;
  
  options.columns = [];
  
  var self = this;
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').each(function() {
    if ($(this).find('input[type="checkbox"]')[0].checked) {
      var name = this.getAttribute('column');
      var fullColumnOptions = self._columnOptionMap[name];
      var columnOptions = {
        name: name,
        reconSettings: $.extend({}, fullColumnOptions.reconSettings),
        dateSettings: $.extend({}, fullColumnOptions.dateSettings)
      };
      if (columnOptions.dateSettings.format != 'custom') {
        delete columnOptions.dateSettings.custom;
      }
      options.columns.push(columnOptions);
    }
  });
  
  return options;
};

