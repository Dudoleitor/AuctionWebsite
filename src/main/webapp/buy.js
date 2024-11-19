"use strict";

const visitedAuctionsKey = "auctionsRecentlyVisited_" +
    sessionStorage.getItem("username");

const saveAuctionAsVisited = function (id) {
    const key = visitedAuctionsKey;
    if (localStorage.getItem(key) != null) {
        const prevVisited = JSON.parse(localStorage.getItem(key));
        if (prevVisited.length == null) {
            if (prevVisited!==id) {
                localStorage.setItem(key, JSON.stringify(
                    [prevVisited, id]
                ));
            }
        } else {
            if(!prevVisited.includes(id)) {
                prevVisited.push(id);
                localStorage.setItem(key, JSON.stringify(prevVisited));
            }
        }

    } else {
        localStorage.setItem(key, JSON.stringify(id));
    }
};

const OpenAuctionsWithSearch = function (
    _outTable,
    _outTableBody,
    _outMessage,
    _handlers
) {
    this.outTable = _outTable;
    this.outTableBody = _outTableBody;
    this.outMessage = _outMessage;

    // False --> called with showPreviouslyVisited
    this.queryPresent = false;
    // True --> user clicked on submit without entering a query
    this.queryEmpty = false;

    this.handlers = _handlers;

    const self = this;

    this.init = function () {
        self.show();
    };

    this.reset = function() {
        this.outTable.style.visibility = "hidden";
    };

    this.update = function (auctions) {
        self.outTableBody.innerHTML = "";
        saveAuctions(auctions);

        auctions.forEach(function (auction){
            const row = document.createElement("tr");

            const idTD = document.createElement("td");
            idTD.textContent = auction.id;
            row.appendChild(idTD);

            const terminatesTD = document.createElement("td");
            terminatesTD.textContent =
                countdownFromLogin(auction.terminates_at);
            row.appendChild(terminatesTD);

            const mindeltaTD = document.createElement("td");
            mindeltaTD.textContent = "€"+auction.minimum_bid_wedge;
            row.appendChild(mindeltaTD);

            const creatorTD = document.createElement("td");
            creatorTD.textContent = auction.creator_user_username;
            row.appendChild(creatorTD);

            const linkTD = document.createElement("td");
            const anchor = document.createElement("a");
            anchor.appendChild(document.createTextNode("Show details"));
            anchor.setAttribute("auction_id", auction.id);
            anchor.href="#";
            self.handlers.forEach(function(handler){
                anchor.addEventListener("click", handler.show, false);
            });
            linkTD.appendChild(anchor);
            row.appendChild(linkTD);

            self.outTableBody.appendChild(row);
        });
        self.outTable.style.visibility = "visible";

        /* Updating recently visited,
        if the list showed recently visited auctions */
        if (self.queryPresent) {return;}
        let visited = [];
        auctions.forEach(function (auction) {
            if(visited.length == null) {
                visited = [visited, auction.id];
            } else {
                visited.push(auction.id);
            }
        });
        localStorage.setItem(visitedAuctionsKey, JSON.stringify(visited));
    };

    this.callback = function (req) {
        const resp = req.responseText;
        switch (req.status) {
            case 200:
                const auctions = JSON.parse(resp);
                if (auctions.length>0) {
                    if(self.queryPresent) {
                        if(self.queryEmpty) {
                            self.outMessage.textContent =
                                "Showing all auctions";
                        } else {
                            self.outMessage.textContent =
                                "Showing auctions matching your query";
                        }
                        self.update(auctions);
                    } else {
                        self.outMessage.textContent =
                            "Showing auctions you recently viewed";
                        self.update(auctions);
                        autoClicker.autoClick(self.outTableBody);
                    }
                } else {
                    if(self.queryPresent) {
                        if(self.queryEmpty) {
                            self.outMessage.textContent =
                                "Showing all auctions";
                        } else {
                            self.outMessage.textContent =
                                "No auction matches your query";
                        }
                    } else {
                        self.outMessage.textContent =
                            "You did not view any auction recently";
                        localStorage.removeItem(visitedAuctionsKey);
                    }
                    self.outTable.style.visibility =
                        "hidden";
                }
                return;
            case 400:  // Bad request, input sanitizer throw an exception
                self.outMessage.textContent =
                    getErrorMessage(resp);
                self.outTable.style.visibility =
                    "hidden";
                return;
            default:
                self.outMessage.textContent =
                    "No auction to show, server error";
                self.outTable.style.visibility =
                    "hidden";
                return;
        }
    };

    this.showPreviouslyVisited = function() {
        const prevVisited =
            JSON.parse(localStorage.getItem(visitedAuctionsKey));

        if(prevVisited == null) {
            self.outMessage.textContent =
                "You did not view any auction recently";
            self.outTable.style.visibility =
                "hidden";
            return;
        }

        let reqParam = "?";
        if ( prevVisited.length == null ) {
            reqParam += "id=" + prevVisited;
        } else {
            prevVisited.forEach(function (id){
                reqParam += "id=" + id + "&";
            });
            reqParam = reqParam.slice(0, reqParam.length - 1);
        }
        makeCallManual("GET", "auctionsFromIds"+reqParam, null, self.callback);
    };

    this.show = function(e) {
        let paramList;
        if (e != null) {
            e.preventDefault();
            setLastAction("searchAuction");

            paramList = {
                q: e.target.q.value
            };
            e.target.reset();
            if (paramList.q != null) {
                self.queryPresent = true;
                self.queryEmpty = paramList.q === "";
            } else {
                self.queryPresent = false;
            }
            makeCall("GET", "openAuctionsWithSearch", paramList, self.callback);
        } else {
            self.showPreviouslyVisited();
        }
    };
};

const ArticlesList = function (
    _outTable,
    _outTableBody,
    _outTitle
) {
    // In additions to these parameters, updateNewBidMinimum must be set
    this.outTable = _outTable;
    this.outTableBody = _outTableBody;
    this.outTitle = _outTitle;
    this.getURL = "articlesByAuctionId";
    this.errorMessage = "No articles to show";
    this.noArticlesMessagge = "No articles for this auction";
    this.auctionId = 0;
    const self = this;

    this.updateNewBidMinimum = function() {};  // Set by someone else

    this.init = function () {
        self.reset();
    };

    this.reset = function() {
        this.outTitle.style.visibility = "hidden";
        this.outTable.style.visibility = "hidden";
    };

    this.callback = function (req) {
        const resp = req.responseText;
        switch (req.status) {
            case 200:
                const articles = JSON.parse(resp);
                if (articles.length>0) {
                    self.update(articles);
                } else {
                    self.outTitle.textContent =
                        self.noArticlesMessagge;
                    self.outTitle.style.visibility = "visible";
                    self.outTable.style.visibility = "hidden";
                }
                return;
            case 400:
                self.outTitle.textContent =
                    self.errorMessage + ": "
                    + getErrorMessage(resp);
                self.outTitle.style.visibility = "visible";
                self.outTable.style.visibility = "hidden";
                return;
            default:
                self.outTitle.textContent =
                    self.errorMessage + ", server error";
                self.outTitle.style.visibility = "visible";
                self.outTable.style.visibility = "hidden";
                return;
        }
    };

    this.update = function (articles) {
        self.outTableBody.innerHTML = "";
        let pricesSum = 0;

        articles.forEach(function (article) {
            const row = document.createElement("tr");

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

            self.outTableBody.appendChild(row);
            pricesSum += article.base_price;
        });
        self.outTitle.textContent = "Articles list";
        self.outTitle.style.visibility = "visible";
        self.outTable.style.visibility = "visible";
        self.updateNewBidMinimum(pricesSum, false);
    };

    this.show = function(e) {
        if (e != null) {
            self.auctionId = e.target.getAttribute("auction_id");
        }
        const paramList = {
            auction: self.auctionId
        };
        makeCall("GET", self.getURL, paramList, self.callback);
    };
};

const AuctionDetails = function (
    _outDiv,
    _outDivBody,
    _outTitle,
    _placeBidDiv,
    _placeBidMessage,
    _placeBidInput
) {
    // In addition to these parameters, bidRefreshCallBack must be set
    this.outDiv = _outDiv;
    this.outDivBody = _outDivBody;
    this.outTitle = _outTitle;
    this.placeBidDiv = _placeBidDiv;
    this.placeBidMessage = _placeBidMessage;
    this.placeBidInput = _placeBidInput;
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
        self.placeBidDiv.style.visibility = "hidden";
    };

    this.update = function (auction) {
        const auctionTerminated = timeIsBeforeNow(auction.terminates_at);

        // Saving the auction in the recentlyVisited list
        if (!auctionTerminated) {
            saveAuctionAsVisited(auction.id);
        }

        self.outTitle.textContent = "Auction #" + auction.id + " - ";
        self.outTitle.textContent +=
            (auctionTerminated ? "Terminated" : "Open");
        self.auction = auction;

        const outSpans = self.outDivBody.getElementsByTagName("span");
        if (outSpans.length>=4) {
            const ownerSPAN = outSpans[0];
            ownerSPAN.textContent = auction.creator_user_username;

            const terminatesSPAN = outSpans[1];
            terminatesSPAN.textContent =
                timeDateFormatter(auction.terminates_at);

            const mindeltaSPAN = outSpans[2];
            mindeltaSPAN.textContent = "€"+auction.minimum_bid_wedge;

            const closedSPAN = outSpans[3];
            if (auction.closed_by_user) {
                closedSPAN.textContent = "Yes";
            } else {
                closedSPAN.textContent = "No";
            }

            self.outDiv.style.visibility = "visible";
            self.placeBidDiv.style.visibility =
                (!auctionTerminated ? "visible" : "hidden");
        }
    };

    this.show = function(e) {
        auctionDetailsShow(e, self);
    };

    this.callback = function(req) {  // To handle the place bid connection
        const resp = req.responseText;
        switch (req.status) {
            case 200:
                self.placeBidMessage.textContent = "Bid placed";
                self.bidPlacedUpdate();
                return;
            case 400:  /* Bad request, user removed constraints from html or
                somebody else placed a bid */
                self.placeBidMessage.textContent =
                    "Bid not placed: " + getErrorMessage(resp);
                self.bidPlacedUpdate();
                return;
            default:
                self.placeBidMessage.textContent =
                    "Server error, bid not placed";
                return;
        }
    };

    this.bidPlacedUpdate = function (){
        const paramList = {
            auction: self.auctionId
        };
        makeCall("GET", "bidsByAuctionId", paramList, self.bidRefreshCallback);
    };

    // Submit event of the placeBid form
    this.handleBidPlacement = function (e) {
        e.preventDefault();
        setLastAction("placeBid");

        if(e.target.checkValidity()) {
            const paramList = {
                auction: self.auctionId,
                bid: e.target.bid.value
            };
            e.target.reset();
            makeCall("POST", "placeBid", paramList, self.callback);
        } else {
            e.target.reportValidity();
        }

    };

    this.updateNewBidMinimum = function(amount, addDelta=true) {
        if(self.placeBidInput!=null&&
            self.auction!=null&&
            amount>self.newBidMinimum) {

            self.newBidMinimum = amount;
            if(addDelta) {
                self.newBidMinimum += self.auction.minimum_bid_wedge;
            }
            self.placeBidInput.setAttribute(
                "min",
                self.newBidMinimum);
        }
    };
};

const WonAuctionsWithArticles = function (
    _outTable,
    _outTableBody,
    _outTitle,
    _handlers
){
    this.outTable = _outTable;
    this.outTableBody = _outTableBody;
    this.outTitle = _outTitle;
    this.handlers = _handlers;
    this.emptyListTitle = "You did not win any auction (yet!)";
    this.defaultTitle = "Auctions you won";
    this.getURL = "wonAuctionsWithArticles";

    const self = this;

    this.init = function () {
        self.show();
    };
    this.reset = function (){
        self.outTitle.style.visibility = "hidden";
    };

    this.update = function (auctionsWithArticles) {
        self.outTableBody.innerHTML = "";
        saveAuctions(auctionsWithArticles);

        auctionsWithArticles.forEach(function (auctionWithArticles) {
            const auctionRow = document.createElement("tr");
            const articles = auctionWithArticles.articleList;
            const articlesCount = articles.length;
            const rowspan = (articlesCount>1 ? articlesCount : 1);

            const auction_idTD = document.createElement("td");
            auction_idTD.textContent = auctionWithArticles.id;
            auction_idTD.setAttribute("rowspan", rowspan);
            auctionRow.appendChild(auction_idTD);

            const auction_terminatedTD = document.createElement("td");
            auction_terminatedTD.textContent =
                timeDateFormatter(auctionWithArticles.terminates_at);
            auction_terminatedTD.setAttribute("rowspan", rowspan);
            auctionRow.appendChild(auction_terminatedTD);

            const auction_creatorTD = document.createElement("td");
            auction_creatorTD.textContent =
                auctionWithArticles.creator_user_username;
            auction_creatorTD.setAttribute("rowspan", rowspan);
            auctionRow.appendChild(auction_creatorTD);


            const auction_maxbidTD = document.createElement("td");
            auction_maxbidTD.textContent = auctionWithArticles.max_bid;
            auction_maxbidTD.setAttribute("rowspan", rowspan);
            auctionRow.appendChild(auction_maxbidTD);

            if(articlesCount>1) {
                let isFirst = true;
                articles.forEach(function (article) {
                    let articleRow;
                    if (isFirst) {
                        articleRow = auctionRow;
                    } else {
                        articleRow = document.createElement("tr");
                    }

                    const nameTD = document.createElement("td");
                    nameTD.textContent = article.name;
                    articleRow.appendChild(nameTD);

                    const descriptionTD = document.createElement("td");
                    descriptionTD.textContent = article.description;
                    articleRow.appendChild(descriptionTD);

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
                        isFirst = false;
                    }
                    self.outTableBody.appendChild(articleRow);
                });

            } else {
                self.outTableBody.appendChild(auctionRow);
            }
        });
        self.outTable.style.visibility = "visible";
    };

    this.callback = function (req) {
        auctionWithArticlesCallback(req, self);
    };

    this.show = function() {
        makeCall("GET", self.getURL, null, self.callback);
    };
};
