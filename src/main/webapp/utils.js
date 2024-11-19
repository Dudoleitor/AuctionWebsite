const logout = function (req) {
	sessionStorage.clear();
	window.location.href = "index.html";
};

const callbackForwarder = function (req, nextCallback) {
	if (req.readyState === XMLHttpRequest.DONE) {
		if (req.status === 403) {  // Forbidden, user is not logged in
			logout(req);
		} else {
			nextCallback(req);
		}
	}
};

const makeCall = function (method, url, paramList, cback) {
	const req = new XMLHttpRequest();
	req.onreadystatechange = function() {
		callbackForwarder(req, cback);
	};
	if (paramList === null) {
		req.open(method, url);
		req.send();
	} else {
		let output = "";
		for (let parameter in paramList) {
			output += parameter + "=" + paramList[parameter] + "&";
		}
		output = output.slice(0, output.length - 1);

		if (method === "GET") {
			url += "?" + output;
		}
			req.open(method, url);
			req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
			req.send(output);
	}
};
const makeCallMultipart = function (url, form, cback) {
	const req = new XMLHttpRequest();
	req.onreadystatechange = function() {
		callbackForwarder(req, cback);
	};
	req.open("POST", url);
	if (form === null) {
		req.send();
	} else {
		req.send(new FormData(form))
	}
};

const makeCallManual = function (method, url, data, cback) {
	const req = new XMLHttpRequest();
	req.onreadystatechange = function() {
		callbackForwarder(req, cback);
	};
	req.open(method, url);
	req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	if (data === null) {
		req.send();
	} else {
		req.send(data)
	}
};

const autoClicker = {
	clickDone: false,

	autoClick: function (div) {
		const divElems = div.getElementsByTagName("a");

		if (
			divElems.length!=null &&
			divElems.length>0 &&
			!this.clickDone  // Set false by initBuy / initSell
		) {
			const e = new Event("click");
			const toClick = divElems[0];
			toClick.dispatchEvent(e);
			this.clickDone = true;
		}
	}
};

const countdownFromLogin = function (date) {
	const timeDate =
		((new Date(date)).getTime())/1000 /3600;
	const timeLoginDate =
		new Date(JSON.parse(sessionStorage.getItem("loginTimestamp")));
	const timeLogin = (timeLoginDate.getTime() /1000) /3600;

	const deltaDays = Math.floor((timeDate - timeLogin)/24);
	const deltaHours = Math.floor((timeDate - timeLogin)%24);

	let output = "";
	if(deltaDays!==0) {output+=deltaDays+"d";}
	if(deltaDays!==0&&deltaHours!==0) {output+=" ";}
	if(deltaHours!==0) {output+=deltaHours+"h";}

	return output;
};

const timeIsBeforeNow = function (time) {
	const timeDate = new Date(time);
	return  timeDate.getTime() < (new Date()).getTime();
};

const timeDateFormatter = function(input) {
	return (new Date(input)).toLocaleString();
};

const auctionDetailsShow = function(e, self) {
	if(e!=null) {e.preventDefault();}
	if (e != null) {
		self.auctionId = parseInt(e.target.getAttribute("auction_id"));
		const auction = getAuction(self.auctionId);
		if(auction!==null) {
			self.auction = auction;
			self.update(self.auction);
		}
	} else {
		self.reset();
	}
};

const auctionWithArticlesCallback = function (req, self) {
	if (req.readyState === XMLHttpRequest.DONE) {
		const resp = req.responseText;
		switch (req.status) {
			case 200:
				const auctionsWithArticles = JSON.parse(resp);
				if(auctionsWithArticles.length>0) {
					self.outTitle.textContent =
						self.defaultTitle;
					self.update(auctionsWithArticles);
					autoClicker.autoClick(self.outTableBody);
				} else {
					self.outTitle.textContent =
						self.emptyListTitle;
					self.outTable.style.visibility = "hidden";
				}
				return;
			case 403:
				logout(req);
				return;
			default:
				self.outTitle.textContent =
					"Server error: no auctions to display";
				self.outTable.style.visibility = "hidden";
		}
	}
};

const getErrorMessage = function(resp) {
	const parser = new DOMParser();
	const parsedResp = parser.parseFromString(resp, "text/html");

	const paragraph = parsedResp.getElementsByTagName("p")[1].textContent;
	return paragraph.slice(8,paragraph.length);
};

const saveAuctions = function (
	auctions
) {
	let keyBase;

	// Finding out if the given block is of closed auctions
	if(auctions[0].closed_by_user) {
		keyBase = "closedAuction_";
	} else {
		keyBase = "auction_";
	}

	auctions.forEach(function (auction){
		const key = keyBase + auction.id;
		const auctionJSON = JSON.stringify(auction);
		sessionStorage.setItem(key, auctionJSON);
	});
};

const getAuction = function (id) {
	const closedKey = "closedAuction_" + id;
	const closedAuction = sessionStorage.getItem(closedKey);

	if (closedAuction!=null) {
		return JSON.parse(closedAuction);
	}

	const notClosedKey = "auction_" + id;
	return JSON.parse(sessionStorage.getItem(notClosedKey));
};

const deleteNotClosedAuction = function (id) {
	const key = "auction_" + id;
	if(sessionStorage.getItem(key) != null) {
		sessionStorage.removeItem(key);
	}
};

const setLastAction = function (action) {
	const username = sessionStorage.getItem("username");
	localStorage.setItem(
		"lastAction_"+username,
		action);
	localStorage.setItem(
		"lastActionTimestamp_"+username,
		JSON.stringify(new Date())
	);
};

const BidsList = function (
	_outTable,
	_outTableBody,
	_outTitle
) {
	// In additions to these parameters, updateNewBidMinimum must be set
	this.outTable = _outTable;
	this.outTableBody = _outTableBody;
	this.outTitle = _outTitle;
	this.defaultTitle = "Bids list";
	this.noBidsTitle = "No bids for this auction";
	this.getURL = "bidsByAuctionId";
	this.auctionId = 0;
	const self = this;

	this.updateNewBidMinimum = function(){};  // Set by someone else

	this.init = function () {
		self.reset();
	};

	this.reset = function() {
		this.outTitle.style.visibility = "hidden";
		this.outTable.style.visibility = "hidden";
	};

	this.callback = function (req) {
		if (req.readyState === XMLHttpRequest.DONE) {
			const resp = req.responseText;
			switch (req.status) {
				case 200:
					const bids = JSON.parse(resp);
					if (bids.length>0) {
						self.update(bids);
					} else {
						self.outTitle.textContent = self.noBidsTitle;
						self.outTitle.style.visibility = "visible";
						self.outTable.style.visibility = "hidden";
					}
					return;
				case 400:  /* Bad request, user removed constraints from html or
                    another error happened */
					self.outTitle.textContent =
						"No bids to show: " + getErrorMessage(resp);
					self.outTitle.style.visibility = "visible";
					self.outTable.style.visibility = "hidden";
					return;
				case 403:
					logout(req);
					return;
				default:
					self.outTitle.textContent =
						"Server error, no bids to show";
					self.outTitle.style.visibility = "visible";
					self.outTable.style.visibility = "hidden";
					return;
			}
		}
	};

	this.update = function (bids) {
		self.outTableBody.innerHTML = "";
		let maxBid = 0;

		bids.forEach(function (bid){
			const row = document.createElement("tr");

			const placedTD = document.createElement("td");
			placedTD.textContent = timeDateFormatter(bid.placed_at);
			row.appendChild(placedTD);

			const bidderTD = document.createElement("td");
			bidderTD.textContent = bid.bidder_user_username;
			row.appendChild(bidderTD);

			const amountTD = document.createElement("td");
			amountTD.textContent = bid.amount;
			row.appendChild(amountTD);

			self.outTableBody.appendChild(row);
			if (bid.amount > maxBid) {
				maxBid = bid.amount;
			}
		});
		self.outTitle.textContent = self.defaultTitle;
		self.outTitle.style.visibility = "visible";
		self.outTable.style.visibility = "visible";
		self.updateNewBidMinimum(maxBid);
	};

	this.refresh = function () {
		if(self.auctionId!==0) {
			self.show();
		} else {
			this.init();
		}
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
