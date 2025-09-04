// Author - Saumya Bhan

(function () {
  document.addEventListener("DOMContentLoaded", function () {
    var root = document.getElementById("bd") || document.body;
    var http = window.location.href;
    var loc;
    if (http.indexOf("share/") !== -1) {
      loc = http.slice(0, http.lastIndexOf("share/"));
    } else {
      // fallback to origin + trailing slash
      loc = window.location.origin + "/";
    }

    // table
    var tbdiv = document.createElement("div");
    tbdiv.style.width = "95%";
    tbdiv.style.borderRadius = "10px";
    tbdiv.style.padding = "1em";
    tbdiv.style.boxShadow = "0px 4px 8px rgba(0, 0, 0, 0.2)";
    tbdiv.style.backgroundColor = "#fff";
    tbdiv.style.margin = "2em";
    tbdiv.style.textAlign = "left";
    tbdiv.style.overflow = "auto";

    var table = document.createElement("table"); // report table
    table.id = "csvTable";
    table.innerHTML = "";
    table.style.width = "100%";
    table.style.borderCollapse = "collapse";
    tbdiv.appendChild(table);

    // message area (status)
    var statusDiv = document.createElement("div");
    statusDiv.style.margin = "8px 0";
    statusDiv.style.fontSize = "14px";
    statusDiv.style.color = "#0f172a";

    // userStatus select
    var userStatus = document.createElement("select");
    userStatus.id = "userStatus";
    userStatus.style.fontFamily = "'Roboto', sans-serif";
    userStatus.style.marginLeft = "0.6em";
    userStatus.style.padding = "10px 3px";
    userStatus.style.border = "2px solid #4A90E2";
    userStatus.style.backgroundColor = "white";
    userStatus.style.borderRadius = "8px";
    userStatus.style.fontSize = "16px";
    userStatus.style.cursor = "pointer";
    userStatus.style.transition = "all 0.3s ease";
    userStatus.style.outline = "none";
    userStatus.style.width = "92px";

    ["all users", "active users", "inactive users"].forEach(function (val) {
      var opt = document.createElement("option");
      // opt.value = val.charAt(0).toUpperCase() + val.slice(1);
      opt.value = val.charAt(0).toUpperCase() + val.split(" ")[0].slice(1);
      opt.text = val.charAt(0).toUpperCase() + val.slice(1);
      userStatus.appendChild(opt);
    });

    // date input
    var dateInput = document.createElement("input");
    dateInput.style.fontFamily = "'Roboto', sans-serif";
    dateInput.type = "date";
    dateInput.id = "sdate";
    dateInput.style.padding = "10px 5px";
    dateInput.style.border = "2px solid #4A90E2";
    dateInput.style.borderRadius = "8px";
    dateInput.style.fontSize = "16px";
    dateInput.style.outline = "none";
    dateInput.style.cursor = "pointer";
    dateInput.style.transition = "all 0.3s ease";
    dateInput.style.marginLeft = "0.6em";
    dateInput.style.backgroundColor = "white";
    dateInput.style.width = "108px";
    dateInput.value = new Date().toISOString().split("T")[0]; // today's date

    // username input
    var userName = document.createElement("input");
    userName.id = "propertyValue";
    userName.placeholder = "Enter username";
    userName.style.width = "200px";
    userName.style.fontFamily = "'Roboto', sans-serif";
    userName.style.marginLeft = "0.6em";
    userName.style.padding = "10px 5px";
    userName.style.border = "2px solid #4A90E2";
    userName.style.backgroundColor = "white";
    userName.style.borderRadius = "8px";
    userName.style.fontSize = "16px";
    userName.style.cursor = "pointer";
    userName.style.outline = "none";

    // Debounce for typing
    let debounceTimer;
    userName.addEventListener("keyup", function () {
      clearTimeout(debounceTimer);
      const txt = userName.value.trim();
      if (txt.length > 0) {
        debounceTimer = setTimeout(() => {
          searchUsers(txt);
        }, 200); // wait 300ms after last keystroke
      } else {
        removeSuggestions();
      }
    });

    // AJAX Call to Alfresco People API
    function searchUsers(txt) {
      fetch(`/alfresco/service/api/people?filter=${encodeURIComponent(txt)}`, {
        method: "GET",
        headers: { Accept: "application/json" },
      })
        .then((res) => res.json())
        .then((data) => {
          // Filter users whose username STARTS with txt (case-insensitive)
          const filtered = data.people.filter((u) =>
            u.userName.toLowerCase().startsWith(txt.toLowerCase())
          );
          showUserSuggestions(filtered);
        })
        .catch((err) => console.error(err));
    }

    function showUserSuggestions(users) {
      removeSuggestions(); // remove old list
      if (users.length === 0) return;

      // ensure parent is positioned so absolute child can be placed correctly
      var parent = userName.parentNode;
      if (getComputedStyle(parent).position === "static") {
        parent.style.position = "relative";
      }

      // create list
      const list = document.createElement("ul");
      list.id = "suggestionList";

      // position the list exactly under the input
      const inputRect = userName.getBoundingClientRect();
      const parentRect = parent.getBoundingClientRect();
      const leftPx = inputRect.left - parentRect.left + "px";
      // const topPx = inputRect.bottom - parentRect.top + "px";
      const topPx = inputRect.bottom - parentRect.top - 4 + "px";

      list.style.position = "absolute";
      list.style.left = leftPx;
      list.style.top = topPx;
      list.style.zIndex = "1000";
      list.style.fontFamily = "'Roboto', sans-serif";
      list.style.width = "211px";
      // list.style.padding = "6px 0";
      list.style.fontSize = "16px";
      list.style.border = "1px solid black";
      // list.style.borderRadius = "8px";
      list.style.backgroundColor = "white";
      list.style.cursor = "pointer";
      list.style.transition = "all 0.15s ease";
      list.style.outline = "none";
      list.style.boxShadow = "0 4px 10px rgba(0,0,0,0.06)";
      list.style.maxHeight = "200px";
      list.style.overflowY = "auto";
      list.style.listStyle = "none";
      list.style.margin = "4px 0 0 0";

      // create items
      users.forEach((u) => {
        const li = document.createElement("li");
        li.textContent = u.userName; // only username
        li.style.padding = "2px 5px";
        li.style.cursor = "pointer";
        li.style.userSelect = "none";
        li.style.whiteSpace = "nowrap";
        li.style.overflow = "hidden";
        li.style.textOverflow = "ellipsis";
        li.style.color = "black";

        // hover + active styling
        li.addEventListener("mouseenter", () => {
          li.style.backgroundColor = "rgb(25, 118, 210)";
          li.style.color = "white";
        });
        li.addEventListener("mouseleave", () => {
          li.style.backgroundColor = "transparent";
          li.style.color = "black";
        });

        li.addEventListener("click", () => {
          userName.value = u.userName; // fill input
          removeSuggestions();
        });

        list.appendChild(li);
      });

      parent.appendChild(list);
    }

    function removeSuggestions() {
      const oldList = document.getElementById("suggestionList");
      if (oldList) oldList.remove();
    }

    // dept dropdown (will be populated)
    var deptDropdown = document.createElement("select");
    deptDropdown.id = "dynamicDropdown";
    deptDropdown.style.fontFamily = "'Roboto', sans-serif";
    deptDropdown.style.width = "226px";
    deptDropdown.style.padding = "10px 5px";
    deptDropdown.style.fontSize = "16px";
    deptDropdown.style.border = "2px solid #4A90E2";
    deptDropdown.style.borderRadius = "8px";
    deptDropdown.style.backgroundColor = "white";
    deptDropdown.style.cursor = "pointer";
    deptDropdown.style.transition = "all 0.3s ease";
    deptDropdown.style.outline = "none";

    // submit button
    var submitButton = document.createElement("button");
    submitButton.id = "submitButton";
    submitButton.textContent = "Submit";
    submitButton.style.fontFamily = "'Roboto', sans-serif";
    submitButton.style.fontSize = "16px";
    submitButton.style.padding = "10px 5px";
    submitButton.style.border = "2px solid #1976d2";
    submitButton.style.borderRadius = "8px";
    submitButton.style.cursor = "pointer";
    submitButton.style.transition = "all 0.3s ease";
    submitButton.style.backgroundColor = "#1976d2";
    submitButton.style.color = "white";
    submitButton.style.boxShadow = "0px 4px 6px rgba(0, 0, 0, 0.1)";
    submitButton.style.marginLeft = "0.6em";
    submitButton.style.outline = "none";

    // download button (hidden until we have data)
    var download = document.createElement("button");
    download.id = "downloadbutton";
    download.textContent = "Download CSV";
    download.style.fontFamily = "'Roboto', sans-serif";
    download.style.fontSize = "16px";
    download.style.padding = "10px 5px";
    download.style.border = "2px solid #04AA6D";
    download.style.borderRadius = "8px";
    download.style.cursor = "pointer";
    download.style.transition = "all 0.3s ease";
    download.style.backgroundColor = "#04AA6D";
    download.style.color = "white";
    download.style.boxShadow = "0px 4px 6px rgba(0, 0, 0, 0.1)";
    download.style.marginLeft = "0.6em";
    download.style.outline = "none";
    download.style.display = "none";

    // small helper: show status
    function setStatus(msg, isError) {
      statusDiv.textContent = msg || "";
      statusDiv.style.color = isError ? "#b91c1c" : "#0f172a";
    }

    // populate sites dropdown
    (function loadSites() {
      var apiUrl = loc + "alfresco/service/api/sites";
      var xhr = new XMLHttpRequest();
      xhr.open("GET", apiUrl);
      xhr.onreadystatechange = function () {
        if (xhr.readyState !== 4) return;
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            var data = JSON.parse(xhr.responseText);

            // "All" option (enabled, always selectable)
            var allOption = document.createElement("option");
            allOption.text = "All Departments";
            allOption.value = "all";
            deptDropdown.appendChild(allOption);

            data
              .sort(function (a, b) {
                return a.title
                  .toLowerCase()
                  .localeCompare(b.title.toLowerCase());
              })
              .forEach(function (site) {
                var option = document.createElement("option");
                option.value = site.shortName;
                option.text = site.title;
                deptDropdown.appendChild(option);
              });
          } catch (e) {
            console.error("Failed to parse sites response", e);
            setStatus("Failed to load sites.", true);
          }
        } else {
          console.error("Failed to fetch sites:", xhr.status, xhr.statusText);
          setStatus("Failed to load sites (" + xhr.status + ")", true);
        }
      };
      xhr.send();
    })();

    // helper: render permissions array into table
    function renderTableFromPermissions(perms, summaryMeta) {
      table.innerHTML = "";

      if (!Array.isArray(perms) || perms.length === 0) {
        table.innerHTML = "<caption>No permissions to display.</caption>";
        download.style.display = "none";
        return;
      }

      // preferred columns & labels
      var columns = [
        "username",
        // "permissionType",
        "groupName",
        "role",
        "site",
        "nodeName",
        "nodePath",
        "userStatus",
        "fromDate",
        "userLogin",
      ];
      var labels = {
        username: "Username",
        // permissionType: "Type",
        groupName: "Group Name",
        role: "Role",
        site: "Site",
        nodeName: "Node Name",
        nodePath: "Node Path",
        userStatus: "User Status",
        fromDate: "From Date",
        userLogin: "User Login",
      };

      // header
      var thead = document.createElement("thead");
      var headRow = document.createElement("tr");
      columns.forEach(function (key) {
        var th = document.createElement("th");
        th.textContent = labels[key] || key;
        th.style.position = "sticky";
        th.style.top = "0";
        th.style.background = "#f8fafc";
        th.style.borderBottom = "2px solid #e5e7eb";
        th.style.padding = "10px 8px";
        th.style.textAlign = "left";
        th.style.whiteSpace = "nowrap";
        th.style.cursor = "pointer";
        headRow.appendChild(th);

        // simple column sort
        (function (colKey) {
          var asc = true;
          th.addEventListener("click", function () {
            perms.sort(function (a, b) {
              var av = a[colKey] == null ? "" : String(a[colKey]);
              var bv = b[colKey] == null ? "" : String(b[colKey]);
              // numeric fallback
              var na = parseFloat(av.replace(/[^\d.-]/g, ""));
              var nb = parseFloat(bv.replace(/[^\d.-]/g, ""));
              if (
                !isNaN(na) &&
                !isNaN(nb) &&
                av.trim() !== "" &&
                bv.trim() !== ""
              ) {
                return asc ? na - nb : nb - na;
              }
              return asc
                ? av.localeCompare(bv, undefined, {
                    numeric: true,
                    sensitivity: "base",
                  })
                : bv.localeCompare(av, undefined, {
                    numeric: true,
                    sensitivity: "base",
                  });
            });
            asc = !asc;
            renderTableFromPermissions(perms, summaryMeta); // re-render sorted table
          });
        })(key);
      });
      thead.appendChild(headRow);
      table.appendChild(thead);

      // body
      var tbody = document.createElement("tbody");
      perms.forEach(function (p, idx) {
        var tr = document.createElement("tr");
        tr.style.backgroundColor = idx % 2 === 0 ? "#ffffff" : "#f9fafb";
        tr.style.borderBottom = "1px solid #eef2f7";

        columns.forEach(function (key) {
          var td = document.createElement("td");
          var val = p[key] != null ? String(p[key]) : "";
          td.textContent = val;
          td.style.padding = "8px";
          td.style.verticalAlign = "top";
          td.style.whiteSpace = "pre-wrap";
          tr.appendChild(td);
        });

        // click to copy nodeRef (optional)
        tr.addEventListener("click", function () {
          if (p.nodeRef) {
            navigator.clipboard?.writeText(p.nodeRef).catch(function () {});
          }
        });

        tbody.appendChild(tr);
      });
      table.appendChild(tbody);

      // enable download button
      download.style.display = "inline-block";
      download.dataset.perms = JSON.stringify(perms); // stash for CSV
      download.dataset.summary = JSON.stringify(summaryMeta || {});
    }

    download.addEventListener("click", function () {
      try {
        var loc = window.location.origin + "/"; // Base server location
        var baseUrl =
          loc + "alfresco/service/alfresco/tutorials/direct-permissions-xlsx";

        // Collect filter values (trim later)
        var selectedDept = (
          (document.getElementById("dynamicDropdown") || {}).value || ""
        ).trim();
        var userStatusValue = (
          (document.getElementById("userStatus") || {}).value || ""
        ).trim();
        var dateInputValue = (
          (document.getElementById("sdate") || {}).value || ""
        ).trim();

        // Robust username finder: try multiple ids / attributes / selectors
        function findUsername() {
          var tryIds = [
            "usernameSearch",
            "username",
            "userName",
            "user-name",
            "usernameInput",
            "user",
          ];

          for (var i = 0; i < tryIds.length; i++) {
            var el = document.getElementById(tryIds[i]);
            if (el)
              return {
                el: el,
                val: (
                  el.value ||
                  el.textContent ||
                  el.getAttribute("data-value") ||
                  ""
                ).trim(),
                selector: "#" + tryIds[i],
              };
          }

          // try common name attributes
          var byName = document.querySelector(
            'input[name="usernameSearch"], input[name="username"], input[name="userName"], input[name="user"]'
          );
          if (byName)
            return {
              el: byName,
              val: (
                byName.value ||
                byName.textContent ||
                byName.getAttribute("data-value") ||
                ""
              ).trim(),
              selector:
                'input[name="' + (byName.getAttribute("name") || "") + '"]',
            };

          // try placeholder heuristics (case-insensitive)
          var byPlaceholder = Array.from(
            document.querySelectorAll("input[placeholder]")
          ).find(function (i) {
            return /user/i.test(i.getAttribute("placeholder"));
          });
          if (byPlaceholder)
            return {
              el: byPlaceholder,
              val: (
                byPlaceholder.value ||
                byPlaceholder.textContent ||
                byPlaceholder.getAttribute("data-value") ||
                ""
              ).trim(),
              selector: "input[placeholder*='user']",
            };

          // last resort: any input inside same filter container (if you have container class)
          var fallback = document.querySelector(
            ".filters input, .filter-row input, .search-controls input"
          );
          if (fallback)
            return {
              el: fallback,
              val: (
                fallback.value ||
                fallback.textContent ||
                fallback.getAttribute("data-value") ||
                ""
              ).trim(),
              selector: "fallback selector",
            };

          return { el: null, val: "", selector: null };
        }

        var found = findUsername();
        var userNameValue = (found.val || "").trim();

        // Build query parameters
        var params = new URLSearchParams();

        // Only add site if not "all" and not empty
        if (selectedDept && selectedDept.toLowerCase() !== "all") {
          params.append("site", selectedDept);
        }

        if (userStatusValue && userStatusValue.toLowerCase() !== "all") {
          params.append("userStatus", userStatusValue);
        }

        // Only append username if non-empty
        if (userNameValue) {
          params.append("usernameSearch", userNameValue);
        }

        if (dateInputValue) {
          params.append("fromDate", dateInputValue);
        }

        // Debug: show what was used to find username and final url
        console.log(
          "Username lookup -> selector:",
          found.selector,
          "element:",
          found.el,
          "value:",
          "[" + userNameValue + "]"
        );
        var fullUrl = params.toString()
          ? baseUrl + "?" + params.toString()
          : baseUrl;
        console.log("Download URL:", fullUrl);

        // Trigger download
        window.location.href = fullUrl;
      } catch (e) {
        console.error("Download failed:", e);
        setStatus("Failed to start download.", true);
      }
    });

    //  submit logic: call API and render
    submitButton.addEventListener("click", function () {
      setStatus("Loading...", false);
      download.style.display = "none";

      // var selectedDept = deptDropdown.value || "";
      // var userStatusValue = userStatus.value || "";
      // var userNameValue = userName.value || "";
      // var dateInputValue = dateInput.value || "";

      var selectedDept = deptDropdown.value;
      var userStatusValue = userStatus.value;
      var userNameValue = userName.value;
      var dateInputValue = dateInput.value;

      // Build URL and optional query params - adapt as per your backend
      var baseUrl =
        loc + "alfresco/service/alfresco/tutorials/direct-permissions";
      var params = new URLSearchParams();
      // if (selectedDept) params.append("site", selectedDept);
      // else params.append("site", "test"); // fallback if you want default site=test

      // Handle site param
      if (selectedDept && selectedDept !== "all") {
        params.append("site", selectedDept);
      }
      // if site=all → skip adding site param completely

      if (userStatusValue && userStatusValue !== "all users")
        params.append("userStatus", userStatusValue);
      if (userNameValue) params.append("usernameSearch", userNameValue);
      if (dateInputValue) params.append("fromDate", dateInputValue);

      var baseUrl = baseUrl + "?" + params.toString();
      console.log("Calling URL:", baseUrl);

      // Use fetch with credentials
      fetch(baseUrl, { credentials: "include" })
        .then(function (res) {
          if (!res.ok)
            throw new Error("Network response was not ok (" + res.status + ")");
          return res.text();
        })
        .then(function (txt) {
          try {
            var data = JSON.parse(txt);
          } catch (e) {
            // Sometimes backend may already return JSON; try direct parse
            try {
              var data = JSON.parse(JSON.stringify(txt));
            } catch (ee) {
              throw new Error("Failed to parse response JSON");
            }
          }
          if (!data || !Array.isArray(data.permissions)) {
            // if response is wrapped differently
            if (Array.isArray(data)) {
              renderTableFromPermissions(data, {}); // direct array
            } else {
              setStatus("Response did not contain permissions array.", true);
              console.error("Unexpected response:", data);
            }
            return;
          }
          renderTableFromPermissions(data.permissions, data);
          setStatus(
            "Loaded " + (data.permissions.length || 0) + " permission rows.",
            false
          );
        })
        .catch(function (err) {
          console.error("Fetch error:", err);
          setStatus("Request failed: " + err.message, true);
        });
    });

    // append controls to page
    root.appendChild(deptDropdown);
    root.appendChild(userStatus);
    root.appendChild(dateInput);
    root.appendChild(userName);
    root.appendChild(submitButton);
    root.appendChild(download);
    root.appendChild(statusDiv);
    root.appendChild(tbdiv);
  });
})();
