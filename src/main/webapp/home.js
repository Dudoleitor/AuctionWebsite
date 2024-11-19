"use strict";
{  // To avoid variables ending up in the global scope
    const PageOrchestrator = function() {
        const self = this;

        this.toInit = function(){};

        this.initBuy = function(pageContent) {
            document.getElementById("sell").innerHTML = "";
            document.getElementById("buy").innerHTML = pageContent;

            // Building objects
            const openAuctionsWithSearchTable =
                document.getElementById("openAuctionsWithSearch_table");
            const wonAuctionsTable =
                document.getElementById("wonAuctions_table");

            const openAuctionsWithSearchForm =
                document.getElementById("openAuctionsWithSearch_form");
            const placeBidForm =
                document.getElementById("placeBid_form");

            const auctionDetailsHandler = new AuctionDetails(
                document.getElementById("auctionDetails_div"),
                document.getElementById("auctionDetails_divBody"),
                document.getElementById("auctionDetails_title"),
                document.getElementById("placeBid_div"),
                document.getElementById("placeBid_message"),
                document.getElementById("placeBid_input")
            );
            const articlesListHandler = new ArticlesList(
                document.getElementById("articlesList_table"),
                document.getElementById("articlesList_body"),
                document.getElementById("articlesList_title")
            );
            const bidsListHandler = new BidsList(
                document.getElementById("bidsList_table"),
                document.getElementById("bidsList_body"),
                document.getElementById("bidsList_title")
            );
            const openAuctionsWithSearchHandler = new OpenAuctionsWithSearch(
                openAuctionsWithSearchTable,
                document.getElementById("openAuctionsWithSearch_body"),
                document.getElementById("openAuctionsWithSearch_message"),
                [auctionDetailsHandler,
                    articlesListHandler,
                    bidsListHandler]
            );
            const wonAuctionsHandler = new WonAuctionsWithArticles(
                wonAuctionsTable,
                document.getElementById("wonAuctions_body"),
                document.getElementById("wonAuctions_title"),
                [auctionDetailsHandler,
                    articlesListHandler,
                    bidsListHandler]
            );

            const handlers = [
                auctionDetailsHandler,
                articlesListHandler,
                bidsListHandler,
                openAuctionsWithSearchHandler,
                wonAuctionsHandler
            ];
            // Completed object building

            // Invoking init and binding handlers
            autoClicker.clickDone = false;
            handlers.forEach(function (handler) {
                handler.init();
            });

            auctionDetailsHandler.bidRefreshCallback =
                bidsListHandler.callback;

            articlesListHandler.updateNewBidMinimum =
                auctionDetailsHandler.updateNewBidMinimum;
            bidsListHandler.updateNewBidMinimum =
                auctionDetailsHandler.updateNewBidMinimum;

            // Adding event listeners
            openAuctionsWithSearchForm.addEventListener(
                "submit",
                openAuctionsWithSearchHandler.show,
                false);
            placeBidForm.addEventListener(
                "submit",
                auctionDetailsHandler.handleBidPlacement,
                false);
        };

        this.initSell = function(pageContent) {
            document.getElementById("buy").innerHTML = "";
            document.getElementById("sell").innerHTML = pageContent;

            const notClosedAuctionsTable =
                document.getElementById("notClosedAuctions_table");
            const closedAuctionsTable =
                document.getElementById("closedAuctions_table");
            const closeAuctionForm =
                document.getElementById("closeAuction_form");
            const addArticleForm =
                document.getElementById("addArticle_form");
            const addAuctionForm =
                document.getElementById("addAuction_form");

            const auctionDetailsHandler = new AuctionDetailsOwner(
                document.getElementById("auctionDetails_div"),
                document.getElementById("auctionDetails_divBody"),
                document.getElementById("auctionDetails_title"),
                document.getElementById("closeAuction_div"),
                document.getElementById("closeAuction_message")
            );
            const bidListHandler = new BidsList(
                document.getElementById("bidsList_table"),
                document.getElementById("bidsList_body"),
                document.getElementById("bidsList_title")
            );
            const notClosedAuctionsHandler = new AuctionsWithArticles(
                notClosedAuctionsTable,
                document.getElementById("notClosedAuctions_body"),
                document.getElementById("notClosedAuctions_title"),
                [auctionDetailsHandler,
                bidListHandler],
                false
            );
            const closedAuctionsHandler = new AuctionsWithArticles(
                closedAuctionsTable,
                document.getElementById("closedAuctions_body"),
                document.getElementById("closedAuctions_title"),
                [auctionDetailsHandler,
                bidListHandler],
                true
            );
            const addArticleHandler = new AddArticle(
                document.getElementById("addArticle_message")
            );
            const addAuctionHandler = new AddAuction(
                addAuctionForm,
                document.getElementById("articlesList_body"),
                document.getElementById("articlesList_title"),
                document.getElementById("addAuction_message")
            );

            const handlers = [
                bidListHandler,
                auctionDetailsHandler,
                addArticleHandler,
                addAuctionHandler,
                notClosedAuctionsHandler,
                closedAuctionsHandler
            ];
            // Completed object building

            // Invoking init and binding handlers
            autoClicker.clickDone = false;
            handlers.forEach(function (handler){
                handler.init();
            });

            auctionDetailsHandler.handlers =
                [notClosedAuctionsHandler,
                    closedAuctionsHandler];
            addArticleHandler.articlesRefresh = addAuctionHandler.show;
            addAuctionHandler.handlers =
                [notClosedAuctionsHandler,
                    closedAuctionsHandler];

            // Adding event listeners
            closeAuctionForm.addEventListener(
                "submit",
                auctionDetailsHandler.handleAuctionClosing,
                false
            );
            addArticleForm.addEventListener(
                "submit",
                addArticleHandler,
                false
            );
            addAuctionForm.addEventListener(
                "submit",
                addAuctionHandler,
                false
            );
        };

        this.callback = function (req) {
            if(req.readyState === XMLHttpRequest.DONE) {
                switch (req.status) {
                    case 200:
                        self.toInit(req.responseText);
                        return;
                    case 403:
                        logout(req);
                        return;
                }
            }
        };

        this.loadBuy = function(e) {
            if(e!=null) {e.preventDefault();}
            self.toInit = self.initBuy;
            makeCall("GET", "buy.html", null, self.callback);
        };

        this.loadSell = function(e) {
            if(e!=null) {e.preventDefault();}
            self.toInit = self.initSell;
            makeCall("GET", "sell.html", null, self.callback);
        };

        this.clearSession = function (){
            sessionStorage.clear();
        };

        this.init = function() {
            const username = sessionStorage.getItem("username");
            document.getElementById("username").textContent = username;
            const navBuy = document.getElementById("nav_buy");
            const navSell = document.getElementById("nav_sell");
            const navLogout = document.getElementById("nav_logout");
            navBuy.addEventListener("click", self.loadBuy, false);
            navSell.addEventListener("click", self.loadSell, false);
            navLogout.addEventListener("click", self.clearSession, false);

            const lastActionTS =
                new Date(localStorage.getItem("lastActionTimestamp_"+username));
            const lastActionTSForward =
                new Date(lastActionTS.setHours( lastActionTS.getHours() + 24*30) );

            if( lastActionTSForward.getTime() < (new Date()).getTime() ) {
                localStorage.clear();
            }

            const lastAction = localStorage.getItem("lastAction_"+username);

            if (lastAction !== "addAuction") {
                self.loadBuy();
            } else {
                self.loadSell();
            }
        };
    };

    // Execution starts here
    const pageOrchestrator = new PageOrchestrator();
    window.addEventListener("load", pageOrchestrator.init, false);
}
