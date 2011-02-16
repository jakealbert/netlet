var powerchart;

function requestData() {
    $.ajax({
	    url: '/charts/power.json?n=42',
		success: function(points) {
		var pointse = eval(points);
		var j = 0;
		for (j = 0; j < pointse.length; j++) {
		    powerchart.series[j].name = pointse[j].name;
		    powerchart.series[j].marker = false;
		    $("g.highcharts-legend path").attr("stroke-width","12").attr("d","M -15 0 L -3 0");
		    $("g.highcharts-legend text:eq("+j+") tspan").text(pointse[j].name);
		    var i = 0;
		    for (i = 0; i < pointse[j].data.length-1; i++) {
			var shift = powerchart.series[j].data.length > 40;
			powerchart.series[j].addPoint(pointse[j].data[i],false,shift);
		    }
		    
		    var shift = powerchart.series[j].data.length > 40;
		    powerchart.series[j].addPoint(pointse[j].data[pointse[j].data.length-1],true,shift);
		}

		for (j = pointse.length; j < powerchart.series.length; j++) {
		    $("g.highcharts-legend text:eq("+j+")").hide();
		    $("g.highcharts-legend path:eq("+j+")").hide();
		}

		},
		cache: false
		});

    setInterval(requestDataUpdate, 1000);

}

function requestDataUpdate() {
	
	
	    var lasttime = powerchart.series[0].data[powerchart.series[0].data.length-1].x;
	    var updateurl = "/charts/power.json?startdt="+lasttime;
    $.ajax({
	    url: updateurl,
		success: function(points) {
		var pointse = eval(points);
		var j = 0;
		for (j = 0; j < pointse.length; j++) {
		    powerchart.series[j].name = pointse[j].name;
		    powerchart.series[j].marker = false;
		    var i = 0;
		    for (i = 0; i < pointse[j].data.length-1; i++) {
			var shift = powerchart.series[j].data.length > 40;
			powerchart.series[j].addPoint(pointse[j].data[i],false,shift);
		    }

		    var shift = powerchart.series[j].data.length > 40;
		    powerchart.series[j].addPoint(pointse[j].data[pointse[j].data.length-1],true,shift);
		}

		},
		cache: false
		});

}



$(document).ready(function(){
	$('form.outlet select').change(function() {
		var newvalue = $(this).attr('value');
		var netlet = $("#"+$(this).attr('id')+"-netlet");
		var outlet = $("#"+$(this).attr('id')+"-outlet");
		var myform = $("#"+$(this).attr('id')+"-submit");
		$.post('/set-outlet',{newvalue: newvalue, netlet: netlet.val(), outlet: outlet.val(), ajax: "true"},function(data) {		$("#"+$(this).attr('id')).value  =  data;});
	    });
	$('form.outlet input.update').hide();


	$('li.track').click(function(){
		$('div.ae-expanded').slideUp("fast").removeClass("ae-expanded");
	    });
	$('div.track-actions a').change(function(event){
		$(this).parent().siblings('div.action-expand').addClass('expanding');
		$('div.expanding').append('<div class="action-expand-triangle"></div>');
		$('div.ae-expanded:not(.expanding) div.action-expand-triangle').remove();
		$('div.ae-expanded:not(.expanding)').slideUp("fast").removeClass('ae-expanded');
		$('div.action-expand-triangle').css({"left" : ($(this).offset().left + $(this).outerWidth()/2 - $(this).parent().offset().left - 10) + "px"});
		$(this).parent().siblings('div.action-expand').slideDown("fast").addClass('ae-expanded').removeClass('expanding');

		$('div.action-expand div:not(.'+$(this).attr('link')+'):not(.action-expand-triangle)').hide();
		$('div.action-expand div.'+$(this).attr('link')).show();
		event.stopPropagation();
		event.preventDefault();
	    });






	try{
	    $('select.calendar').selectToUISlider({labels: 0 });
	    $('select.calendar').click(function(){
		    $('#current-chart-container img').attr("src","/charts/current.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#power-chart-container img').attr("src","/charts/power.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#devices-chart-container img').attr("src","/charts/devices.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		});
	    $('a#handle_startdate.ui-slider-handle, a#handle_enddate.ui-slider-handle').mouseup(function(){
		    $('#current-chart-container img').attr("src","/charts/current.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#power-chart-container img').attr("src","/charts/power.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#devices-chart-container img').attr("src","/charts/devices.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		});
	    $('a#handle_startdate.ui-slider-handle, a#handle_enddate.ui-slider-handle').hover(function(){
		    $('#current-chart-container img').attr("src","/charts/current.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#power-chart-container img').attr("src","/charts/power.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#devices-chart-container img').attr("src","/charts/devices.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		});
	    $('a#handle_startdate.ui-slider-handle, a#handle_enddate.ui-slider-handle').change(function(){
		    $('#current-chart-container img').attr("src","/charts/current.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#power-chart-container img').attr("src","/charts/power.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		    $('#devices-chart-container img').attr("src","/charts/devices.png?startdt="+ $('#startdate').val() +"&enddt="+$('#enddate').val());
		});
	} catch (err) { }
	$('div.ui-slider').before('<div class="span-16 prepend-1 last" id="calslider"></div>');
	$('div#calslider').append($('div.ui-slider')).height('70px');
	$('div#calslider').position('absolute');

	
	powerchart = new Highcharts.Chart({
		colors: ["#8D361A","#BE6F2D","#E3BE4B","#9CAA3B","#43621E"],
      chart: {
         renderTo: 'ticker-chart-container',
	 zoomType: 'x',
         defaultSeriesType: 'spline',
         marginRight: 85,
	 marginLeft: 50,
	 marginTop: 5,
         marginBottom: 35,
	 plotShadow: true,
	 events: {
			load: requestData
			
	 },
	 backgroundColor: null
      },
      title: {
	    text: null
      },
      xAxis: {
		    title: {
			text: 'Time'
		    },
		    type: 'datetime',
		    tickPixelInterval: 120,
		    maxZoom: 40,
	    gridLineWidth: 0,
	   
      },
      yAxis: {
         title: {
            text: 'Power (kW)'
         },
	 minPadding: 0.1,
	 maxPadding: 0.1,
         plotLines: [{
            value: 0,
            width: 1,
            color: '#808080'
         }]
      },
      tooltip: {
         formatter: function() {
                   return '<b>'+ this.series.name +'</b><br/>'+
               this.x +': '+ this.y +'kW';
         }
      },
      legend: {
         layout: 'vertical',
         align: 'right',
         verticalAlign: 'middle',
        
         borderWidth: 0,
	 enabled: true,
	 width: 90,
	 height: 150
      },
      series: [{
			name: '',
			data: [],
			marker: { enabled: false }
		    },
	       {
			name: '',
			data: [],
			marker: { enabled: false }
	       },
	       {
			name: '',
			data: [],
			marker: { enabled: false }
	       },
	       {
			name: '',
			data: [],
			marker: { enabled: false }
	       },
{
			name: '',
			data: [],
			marker: { enabled: false }
	       },
{
			name: '',
			data: [],
			marker: { enabled: false }
	       }
	  
	       ]
	    });



 });
