var powerchart;

function requestData() {
    $.ajax({
	    url: '/charts/power.json?outlets=1,2',
		success: function(points) {
		var pointse = eval(points);
		var j = 0;
		for (j = 0; j < pointse.length; j++) {
		    powerchart.series[j].name = pointse[j].name;
		    powerchart.series[j].marker = pointse[j].marker;
		    var i = 0;
		    for (i = 0; i < pointse[j].data.length-1; i++) {
			var shift = powerchart.series[j].data.length > 80;
			powerchart.series[j].addPoint(pointse[j].data[i],false,shift);
		    }
		    
		    var shift = powerchart.series[j].data.length > 80;
		    powerchart.series[j].addPoint(pointse[j].data[pointse[j].data.length-1],true,shift);
		}

		},
		cache: false
		});

    setInterval(requestDataUpdate, 1000);

}

function requestDataUpdate() {
	
	
	    var lasttime = powerchart.series[0].data[powerchart.series[0].data.length-1].x;
	    var updateurl = "/charts/power.json?outlets=1,2&startdt="+lasttime;
    $.ajax({
	    url: updateurl,
		success: function(points) {
		var pointse = eval(points);
		var j = 0;
		for (j = 0; j < pointse.length; j++) {
		    powerchart.series[j].name = pointse[j].name;
		    powerchart.series[j].marker = pointse[j].marker;
		    var i = 0;
		    for (i = 0; i < pointse[j].data.length-1; i++) {
			var shift = powerchart.series[j].data.length > 80;
			powerchart.series[j].addPoint(pointse[j].data[i],false,shift);
		    }

		    var shift = powerchart.series[j].data.length > 80;
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
	$('div.track-actions a').click(function(event){
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
	} catch (err) { }
	$('div.ui-slider').before('<div class="span-16 prepend-1 last" id="calslider"></div>');
	$('div#calslider').append($('div.ui-slider')).height('70px');
	$('div#calslider').position('absolute');

	
	powerchart = new Highcharts.Chart({
      chart: {
         renderTo: 'power-chart-container',
	 zoomType: 'x',
         defaultSeriesType: 'area',
         marginRight: 0,
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
		    maxZoom: 80,
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
         verticalAlign: 'top',
         x: -10,
         y: 100,
         borderWidth: 0,
	 enabled: false,
	 width: 0
      },
      series: [{
			name: 'Data0',
			data: [],
			marker: { enabled: false }
		    },
	       {
			name: 'Data1',
			data: [],
			marker: { enabled: false }
	       },
	       {
			name: 'Data2',
			data: [],
			marker: { enabled: false }
	       },
	       {
			name: 'Data3',
			data: [],
			marker: { enabled: false }
	       },
	       {
			name: 'Data4',
			data: [],
			marker: { enabled: false }
	       },
	       {
			name: 'Data5',
			data: [],
			marker: { enabled: false }
	       }]
	    });



 });
