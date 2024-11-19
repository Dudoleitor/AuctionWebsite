"use strict";
const AuctionsWithArticles = function (
    _outTable,
    _outTableBody,
    _outTitle,
    _handlers,
    _openVsClosed  // False -> notClosed, True -> closed
){
    this.outTable = _outTable;
    this.outTableBody = _outTableBody;
    this.outTitle = _outTitle;
    this.handlers = _handlers;
    this.openVsClosed = _openVsClosed;
    if(this.openVsClosed) {
        this.emptyListTitle = "You don't have any closed auction";
        this.defaultTitle = "Closed auctions list";
    } else {
        this.emptyListTitle = "You don't have any not closed auction";
        this.defaultTitle = "Not closed auctions list";
    }
    this.getURL = "auctionsForOwner";

    const self = this;

    this.init = function () {
        self.show();
    };
    this.reset = function (){
        self.outTitle.style.visibility = "hidden";
    };

    this.update = function (auctionsWithArticles) {
        self.outTableBody.innerHTML = "";
        self.handlers.forEach(function (handler){
            handler.refresh();
        });
        saveAuctions(auctionsWithArticles);

        auctionsWithArticles.forEach(function (auctionWithArticles) {
            const auctionRow = document.createElement("tr");
            const articles = auctionWithArticles.articleList;
            const articlesCount = articles.length;
            const rowspan = (articlesCount>1 ? articlesCount : 1);
            const auctionTerminated =
                timeIsBeforeNow(auctionWithArticles.terminates_at);

            const auction_idTD = document.createElement("td");
            auction_idTD.textContent = auctionWithArticles.id;
            auction_idTD.setAttribute("rowspan", rowspan);
            auctionRow.appendChild(auction_idTD);

            const auction_terminatedTD = document.createElement("td");
            auction_terminatedTD.textContent =
                ( auctionTerminated ?
                    timeDateFormatter(auctionWithArticles.terminates_at) :
                    countdownFromLogin(auctionWithArticles.terminates_at) );

            auction_terminatedTD.setAttribute("rowspan", rowspan);
            auctionRow.appendChild(auction_terminatedTD);

            if(!self.openVsClosed) {
                const auction_terminatedFlagTD = document.createElement("td");
                auction_terminatedFlagTD.textContent =
                    (auctionTerminated ? "Yes" : "No");
                auction_terminatedFlagTD.setAttribute("rowspan", rowspan);
                auctionRow.appendChild(auction_terminatedFlagTD);
            }

            const auction_maxbidTD = document.createElement("td");
            const maxBid = ( auctionWithArticles.closed_by_user ?
                auctionWithArticles.final_bid_amount :
                auctionWithArticles.max_bid );
            auction_maxbidTD.textContent =
                (maxBid > 0 ? "€"+maxBid : "no bids");
            auction_maxbidTD.setAttribute("rowspan", rowspan);
            auctionRow.appendChild(auction_maxbidTD);

            const appendLink = function(articleRow) {
                const auctionLinkTD = document.createElement("td");
                auctionLinkTD.setAttribute("rowspan", rowspan);
                const anchor = document.createElement("a");
                anchor.appendChild(
                    document.createTextNode("Show details")
                );
                anchor.setAttribute(
                    "auction_id",
                    auctionWithArticles.id
                );
                anchor.href="#";
                self.handlers.forEach(function(handler){
                    anchor.addEventListener(
                        "click",
                        handler.show,
                        false);
                });
                auctionLinkTD.appendChild(anchor);
                articleRow.appendChild(auctionLinkTD);
            };

            if(articlesCount>1) {
                let isFirst = true;
                articles.forEach(function (article) {
                    let articleRow;
                    if (isFirst) {
                        articleRow = auctionRow;
                    } else {
                        articleRow = document.createElement("tr");
                    }

                    const idTD = document.createElement("td");
                    idTD.textContent = article.id;
                    articleRow.appendChild(idTD);

                    const nameTD = document.createElement("td");
                    nameTD.textContent = article.name;
                    articleRow.appendChild(nameTD);

                    if (article.imageBase64 === "") {
                        articleRow.appendChild(document.createElement("td"));
                    } else {
                        const imgTD = document.createElement("td");
                        const img = document.createElement("img");
                        img.setAttribute("src", article.imageBase64);
                        img.setAttribute("height", 120);
                        imgTD.appendChild(img);
                        articleRow.appendChild(imgTD);
                    }

                    if(isFirst) {
                        appendLink(articleRow);
                        isFirst = false;
                    }
                    self.outTableBody.appendChild(articleRow);
                });

            } else {
                auctionRow.appendChild(
                    document.createElement("td"));  // Article ID
                auctionRow.appendChild(
                    document.createElement("td"));  // Article name
                auctionRow.appendChild(
                    document.createElement("td"));  // Article image
                appendLink(auctionRow);
                self.outTableBody.appendChild(auctionRow);
            }
        });
        self.outTable.style.visibility = "visible";
    };

    this.callback = function (req) {
        auctionWithArticlesCallback(req, self);
    };

    this.show = function() {
        const param = {
            openVsClosed: self.openVsClosed
        };
        makeCall("GET", self.getURL, param, self.callback);
    };
};

const AuctionDetailsOwner = function (
    _outDiv,
    _outDivBody,
    _outTitle,
    _closeAuctionDiv,
    _closeAuctionMessage
) {
    // In addition to these parameters, handlers must be set
    this.outDiv = _outDiv;
    this.outDivBody = _outDivBody;
    this.outTitle = _outTitle;
    this.closeAuctionDiv = _closeAuctionDiv;
    this.closeAuctionMessage = _closeAuctionMessage;
    this.handlers = [];  // These are refreshed when the auction is closed
    this.auctionId = 0;
    this.auction = null;

    // Needed by the two methods updateNewBidMinimum
    this.newBidMinimum = 0;

    const self = this;

    this.bidRefreshCallback = function(){};  // Will be set by somebody else

    this.init = function () {
        self.reset();
    };

    this.reset = function() {
        self.outDiv.style.visibility = "hidden";
        self.closeAuctionDiv.style.visibility = "hidden";
    };

    this.update = function (auction) {
        const auctionTerminated = timeIsBeforeNow(auction.terminates_at);
        self.outTitle.textContent = "Auction #" + auction.id + " - ";
        self.outTitle.textContent +=
            (auctionTerminated ? "Terminated" : "Open");
        self.auction = auction;

        const outSpans = self.outDivBody.getElementsByTagName("span");
        if (outSpans.length>=9) {
            const terminatesSPAN = outSpans[0];
            terminatesSPAN.textContent =
                timeDateFormatter(auction.terminates_at);

            const mindeltaSPAN = outSpans[1];
            mindeltaSPAN.textContent = "€"+auction.minimum_bid_wedge;

            const closedSPAN = outSpans[2];
            const winnerUserSPAN = outSpans[4];
            const winningBidSPAN = outSpans[6];
            const winnerAddressSPAN = outSpans[8];
            if (auction.closed_by_user) {
                closedSPAN.textContent = "Yes";

                outSpans[3].style.visibility = "visible";
                winnerUserSPAN.textContent =
                    auction.winner_user_username;

                outSpans[5].style.visibility = "visible";
                winningBidSPAN.textContent =
                    "€"+auction.final_bid_amount;

                outSpans[7].style.visibility = "visible";
                winnerAddressSPAN.textContent =
                    auction.winner_user_address;
            } else {
                closedSPAN.textContent = "No";

                outSpans[3].style.visibility = "hidden";
                winnerUserSPAN.textContent = "";

                outSpans[5].style.visibility = "hidden";
                winningBidSPAN.textContent = "";

                outSpans[7].style.visibility = "hidden";
                winnerAddressSPAN.textContent = "";
            }

            self.outDiv.style.visibility = "visible";
            self.closeAuctionDiv.style.visibility =
                ( (auctionTerminated && !auction.closed_by_user) ?
                    "visible" : "hidden");
        }
    };

    this.refresh = function (){
        if(self.auctionId!==0) {
            const auction = getAuction(self.auctionId);
            if(auction!==null) {
                self.auction = auction;
                self.update(self.auction);
                return;
            }
        }
        self.init();
    };

    this.show = function (e){
        auctionDetailsShow(e, self);
    };

    this.callback = function(req) {  // To handle the auction closed connection
        const resp = req.responseText;
        switch (req.status) {
            case 200:
                self.closeAuctionMessage.textContent = "Auction closed";
                self.auctionClosedUpdate();
                return;
            case 400:  /* Bad request, user removed constraints from html or
                another error happened */
                self.closeAuctionMessage.textContent =
                    "Error while closing auction: " + getErrorMessage(resp);
                return;
            default:
                self.closeAuctionMessage.textContent =
                    "Server error while closing auction";
                return;
        }
    };

    this.auctionClosedUpdate = function (){
        self.handlers.forEach(function(handler){
            handler.show();
        });
        self.update(self.auction);
        deleteNotClosedAuction(self.auctionId);
    };

    // Submit event of the closeAuction form
    this.handleAuctionClosing = function (e) {
        e.preventDefault();
        setLastAction("closeAuction");

        if(e.target.checkValidity()) {
            const paramList = {
                auction: self.auctionId
            };
            makeCall("POST", "closeAuction", paramList, self.callback);
        } else {
            e.target.reportValidity();
        }

    };
};

const AddArticle = function(
    _outMessage
){
    // In addition to these parameters, articlesRefresh must be set
    this.outMessage = _outMessage;
    this.messageSuccess = "Article added";
    const self = this;

    this.articlesRefresh = function (){};  // Will be set by somebody else

    this.init = function() {
        self.outMessage.textContent = "";
    };

    this.reset = function() {
        self.init();
    };

    this.callback = function (req) {
        const resp = req.responseText;
        switch (req.status) {
            case 200:
                self.outMessage.textContent = self.messageSuccess;
                self.articlesRefresh();
                return;
            case 400:
                self.outMessage.textContent =
                    "Article not added: " + getErrorMessage(resp);
                return;
            default:
                self.outMessage.textContent = "Server error, article not added";
                return;
        }
    };

    this.handleEvent = function (e) {
        e.preventDefault();
        setLastAction("addArticle");

        if(e.target.checkValidity()) {
        makeCallMultipart("addArticle", e.target, self.callback);
        e.target.reset();
        } else {
            e.target.reportValidity();
        }
    };
};

const AddAuction = function (
    _outArticlesTable,
    _outArticlesTableBody,
    _outArticlesTitle,
    _outMessage
) {
    // In addition to these parameters, handlers must be set
    this.outArticlesTable = _outArticlesTable;
    this.outArticlesBody = _outArticlesTableBody;
    this.outArticlesTitle = _outArticlesTitle;
    this.outMessage = _outMessage;
    this.getURL = "availableArticles";
    this.postURL = "addAuction";
    this.articlesErrorMessage =
        "Can't create auction: no available articles to include";
    this.handlers = [];  // Set by somebody else
    const self = this;

    this.init = function() {
        self.show();
        self.outMessage.textContent = "";
    };

    this.reset = function() {
        self.outMessage.textContent = "";
        this.outArticlesTable.style.visibility = "hidden";
        this.outArticlesTitle.style.visibility = "hidden";
    };

    this.articlesListCallback = function (req) {
        const resp = req.responseText;
        switch (req.status) {
            case 200:
                const articles = JSON.parse(resp);
                if (articles.length>0) {
                    self.update(articles);
                } else {
                    self.outArticlesTitle.textContent =
                        self.articlesErrorMessage;
                    self.outArticlesTitle.style.visibility = "visible";
                    self.outArticlesTable.style.visibility = "hidden";
                }
                return;
            default:
                self.outArticlesTitle.textContent =
                    self.articlesErrorMessage + ", server error";
                self.outArticlesTitle.style.visibility = "visible";
                self.outArticlesTable.style.visibility = "hidden";
                return;
        }
    };

    this.addAuctionCallback = function (req) {
        const resp = req.responseText;
        switch (req.status) {
            case 200:
                self.outMessage.textContent = "Auction created";
                self.show();
                self.handlers.forEach(function (handler){handler.show();});
                return;
            case 400:
                self.outMessage.textContent =
                    "Error while adding auction: " + getErrorMessage(resp);
                return;
            case 403:
                logout(req);
                return;
            default:
                self.outMessage.textContent =
                    "Server error while adding auction";
                self.show();
                return;
        }
    };

    this.update = function (articles) {
        self.outArticlesBody.innerHTML = "";

        articles.forEach(function (article) {
            const row = document.createElement("tr");

            const flagTD = document.createElement("td");
            const inputElem = document.createElement("input");
            inputElem.setAttribute("type", "checkbox");
            inputElem.setAttribute("name", "articlesId");
            inputElem.setAttribute("value", article.id);
            flagTD.appendChild(inputElem);
            row.appendChild(flagTD);

            const priceTD = document.createElement("td");
            priceTD.textContent = "€"+article.base_price;
            row.appendChild(priceTD);

            const nameTD = document.createElement("td");
            nameTD.textContent = article.name;
            row.appendChild(nameTD);

            const descriptionTD = document.createElement("td");
            descriptionTD.textContent = article.description;
            row.appendChild(descriptionTD);

            if (article.imageBase64 === "") {
                row.appendChild(document.createElement("td"));
            } else {
                const imgTD = document.createElement("td");
                const img = document.createElement("img");
                img.setAttribute("src", article.imageBase64);
                img.setAttribute("height", 120);
                imgTD.appendChild(img);
                row.appendChild(imgTD);
            }

            self.outArticlesBody.appendChild(row);
        });
        self.outArticlesTitle.textContent = "Articles to include:";
        self.outArticlesTitle.style.visibility = "visible";
        self.outArticlesTable.style.visibility = "visible";
    };

    this.show = function() {
        makeCall("GET", self.getURL, null, self.articlesListCallback);
    };

    // Submit event of the addAuction form
    this.handleEvent = function (e) {
        e.preventDefault();
        setLastAction("addAuction");

        let paramString = "";
        if(e.target.checkValidity()) {
            paramString += "terminatesAt=";
            paramString += e.target.terminatesAt.value;

            paramString += "&minBidDelta=";
            paramString += e.target.minBidDelta.value;

            const nodes = e.target.articlesId;
            let atLeastOneChecked = false;
            if(nodes != null) {
                if(nodes.length != null) {
                    for (let i in nodes) {
                        const node = nodes[i];
                        if (node.checked) {
                            paramString += "&articlesId=";
                            paramString += node.value;
                            atLeastOneChecked = true;
                        }
                    }
                } else {
                    const node = nodes;
                    if (node.checked) {
                        paramString += "&articlesId=";
                        paramString += node.value;
                        atLeastOneChecked = true;
                    }
                }
            }
            if (atLeastOneChecked) {
                e.target.reset();
                makeCallManual(
                    "POST",
                    self.postURL,
                    paramString,
                    self.addAuctionCallback
                );
            } else {
                self.outMessage.textContent =
                    "Select at least one article to create an auction";
            }
        } else {
            e.target.reportValidity();
        }
    };
};
