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

DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
    var doReconcile = function() {
        new ReconDialog(column);
    };

    var doReconDiscardJudgments = function() {
        Refine.postCoreProcess(
            "recon-discard-judgments",
            { columnName: column.name },
            null,
            { cellsChanged: true, columnStatsChanged: true }
        );
    };

    var doReconMatchBestCandidates = function() {
        Refine.postCoreProcess(
            "recon-match-best-candidates",
            { columnName: column.name },
            null,
            { cellsChanged: true, columnStatsChanged: true }
        );
    };

    var doReconMarkNewTopics = function(shareNewTopics) {
        Refine.postCoreProcess(
            "recon-mark-new-topics",
            { columnName: column.name, shareNewTopics: shareNewTopics },
            null,
            { cellsChanged: true, columnStatsChanged: true }
        );
    };

    var doSearchToMatch = function() {
        var frame = DialogSystem.createDialog();
        frame.width("400px");

        var header = $('<div></div>').addClass("dialog-header").text("Search for Match").appendTo(frame);
        var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
        var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

        $('<p></p>').text("Search Freebase for a topic to match all filtered cells:").appendTo(body);

        var input = $('<input />').appendTo($('<p></p>').appendTo(body));

        input.suggest({}).bind("fb-select", function(e, data) {
            var query = {
                "id" : data.id,
                "type" : []
            };
            var baseUrl = "http://api.freebase.com/api/service/mqlread";
            var url = baseUrl + "?" + $.param({ query: JSON.stringify({ query: query }) }) + "&callback=?";

            $.getJSON(
                url,
                null,
                function(o) {
                    var types = "result" in o ? o.result.type : [];

                    Refine.postCoreProcess(
                        "recon-match-specific-topic-to-cells",
                        {
                            columnName: column.name,
                            topicID: data.id,
                            topicGUID: data.guid,
                            topicName: data.name,
                            types: types.join(",")
                        },
                        null,
                        { cellsChanged: true, columnStatsChanged: true }
                    );

                    DialogSystem.dismissUntil(level - 1);
                }
            );
        });

        $('<button class="button"></button>').text("Cancel").click(function() {
            DialogSystem.dismissUntil(level - 1);
        }).appendTo(footer);

        var level = DialogSystem.showDialog(frame);
        input.focus().data("suggest").textchange();
    };
    
    MenuSystem.appendTo(menu, [ "core/reconcile" ], [
        {
            id: "core/reconcile",
            label: "Start Reconciling ...",
            tooltip: "Reconcile text in this column with topics on Freebase",
            click: doReconcile
        },
        {},
        {
            id: "core/facets",
            label: "Facets",
            submenu: [
                {
                    id: "core/by-judgment",
                    label: "By Judgment",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.judgment",
                                "omitError" : true
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {},
                {
                    id: "core/by-best-candidates-score",
                    label: "Best Candidate's Score",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.best.score",
                                "mode" : "range"
                            },
                            {
                            }
                        );
                    }
                },
                {
                    id: "core/by-best-candidates-type-match",
                    label: "Best Candidate's Type Match",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.typeMatch",
                                "omitError" : true
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {
                    id: "core/by-best-candidates-name-match",
                    label: "Best Candidate's Name Match",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.nameMatch",
                                "omitError" : true
                            },
                            {
                                "scroll" : false
                            }
                        );
                    }
                },
                {},
                {
                    id: "core/by-best-candidates-name-edit-distance",
                    label: "Best Candidate's Name Edit Distance",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.nameLevenshtein",
                                "mode" : "range"
                            },
                            {
                            }
                        );
                    }
                },
                {
                    id: "core/by-best-candidates-name-word-similarity",
                    label: "Best Candidate's Name Word Similarity",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "range", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.nameWordDistance",
                                "mode" : "range"
                            },
                            {
                            }
                        );
                    }
                },
                {},
                {
                    id: "core/by-best-candidates-types",
                    label: "Best Candidate's Types",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name,
                                "columnName" : column.name, 
                                "expression" : "cell.recon.best.type",
                                "omitError" : true
                            }
                        );
                    }
                }
            ]
        },
        {
            id: "core/qa-facets",
            label: "QA Facets",
            submenu: [
                {
                    id: "core/by-qa-results",
                    label: "QA Results",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name + " QA Results",
                                "columnName" : column.name, 
                                "expression" : "cell.recon.features.qaResult"
                            }
                        );
                    }
                },
                {
                    id: "core/by-judgment-actions",
                    label: "Judgment Actions",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name + " Judgment Actions",
                                "columnName" : column.name, 
                                "expression" : "cell.recon.judgmentAction"
                            }
                        );
                    }
                },
                {
                    id: "core/by-judgment-history-entries",
                    label: "Judgment History Entries",
                    click: function() {
                        ui.browsingEngine.addFacet(
                            "list", 
                            {
                                "name" : column.name + " History Entries",
                                "columnName" : column.name, 
                                "expression" : "cell.recon.judgmentHistoryEntry"
                            }
                        );
                    }
                }
            ]
        },
        {
            id: "core/actions",
            label: "Actions",
            submenu: [
                {
                    id: "core/match-to-best-candidate",
                    label: "Match Each Cell to Its Best Candidate",
                    tooltip: "Match each cell to its best candidate in this column for all current filtered rows",
                    click: doReconMatchBestCandidates
                },
                {
                    id: "core/match-to-new-topic",
                    label: "Create a New Topic for Each Cell",
                    tooltip: "Mark to create one new topic for each cell in this column for all current filtered rows",
                    click: function() {
                        doReconMarkNewTopics(false);
                    }
                },
                {},
                {
                    id: "core/match-similar-to-new-topic",
                    label: "Create One New Topic for Similar Cells",
                    tooltip: "Mark to create one new topic for each group of similar cells in this column for all current filtered rows",
                    click: function() {
                        doReconMarkNewTopics(true);
                    }
                },
                {
                    id: "core/match-to-specific",
                    label: "Match All Filtered Cells to ...",
                    tooltip: "Search for a topic to match all filtered cells to",
                    click: doSearchToMatch
                },
                {},
                {
                    id: "core/discard-judgments",
                    label: "Discard Reconciliation Judgments",
                    tooltip: "Discard reconciliaton judgments in this column for all current filtered rows",
                    click: doReconDiscardJudgments
                }
            ]
        }
    ]);
});