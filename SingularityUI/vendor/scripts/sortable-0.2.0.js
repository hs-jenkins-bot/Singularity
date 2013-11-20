/*! sortable.js 0.2.0 */
(function(){var a,b,c,d,e;b="ascending",c="descending",d=/^-?[£$¤]?[\d,.]+%?$/,e=/^\s+|\s+$/g,a={init:function(b){var c,d,e,f,g;if(1===b.tHead.rows.length){for(e=b.querySelectorAll("th"),c=f=0,g=e.length;g>f;c=++f)d=e[c],"false"!==d.getAttribute("data-sort")&&a.setupClickableTH(b,d,c);return b}},setupClickableTH:function(d,e,f){var g;return g=a.getColumnType(d,f),e.addEventListener("click",function(){var h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w;for(l="true"===this.getAttribute("data-sorted"),m=this.getAttribute("data-sorted-direction"),h=l?m===b?c:b:g.defaultSortDirection,o=this.parentNode.querySelectorAll("th"),p=0,s=o.length;s>p;p++)e=o[p],e.setAttribute("data-sorted","false"),e.removeAttribute("data-sorted-direction");for(this.setAttribute("data-sorted","true"),this.setAttribute("data-sorted-direction",h),n=d.tBodies[0],j=[],v=n.rows,q=0,t=v.length;t>q;q++)i=v[q],j.push([a.getNodeValue(i.cells[f]),i]);for(l?j.reverse():j.sort(g.compare),w=[],r=0,u=j.length;u>r;r++)k=j[r],w.push(n.appendChild(k[1]));return w})},getColumnType:function(b,c){var e,f,g,h,i;for(i=b.tBodies[0].rows,g=0,h=i.length;h>g;g++)if(e=i[g],f=a.getNodeValue(e.cells[c]),""!==f&&f.match(d))return a.types.numeric;return a.types.alpha},getNodeValue:function(a){return a?null!==a.getAttribute("data-value")?a.getAttribute("data-value"):"undefined"!=typeof a.innerText?a.innerText.replace(e,""):a.textContent.replace(e,""):""},types:{numeric:{defaultSortDirection:c,compare:function(a,b){var c,d;return c=parseFloat(a[0].replace(/[^0-9.-]/g,"")),d=parseFloat(b[0].replace(/[^0-9.-]/g,"")),isNaN(c)&&(c=0),isNaN(d)&&(d=0),d-c}},alpha:{defaultSortDirection:b,compare:function(a,b){var c,d;return c=a[0].toLowerCase(),d=b[0].toLowerCase(),c===d?0:d>c?-1:1}}}},window.SorTable=a}).call(this);