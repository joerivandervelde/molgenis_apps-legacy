<#macro QtlFinderHD screen>

	<#-- normally you make one big form for the whole plugin -->
	<form method="post" enctype="multipart/form-data" name="${screen.name}" action="">
	
		<#-- needed in every form: to redirect the request to the right screen -->
		<input type="hidden" name="__target" value="${screen.name}">
		
		<#--needed in every form: to define the action. This can be set by the submit button -->
		<input type="hidden" name="__action">
		
		<#-- need to be set to "true" in order to force a download -->
		<input type="hidden" name="__show">
		
		<input type="hidden" name="__shopMeName">
		<input type="hidden" name="__shopMeId">
		
		<#-- this shows a title and border -->
		<div class="formscreen">
			<div class="form_header" id="${screen.getName()}">
			${screen.label}
			</div>
			
			<#--optional: mechanism to show messages-->
			<#list screen.getMessages() as message>
				<#if message.success>
					<p class="successmessage">${message.text}</p>
				<#else>
					<p class="errormessage">${message.text}</p>
				</#if>
			</#list>
			
			<#--====================================-->
			<#if screen.myModel?exists>
				<#assign modelExists = true>
				<#assign model = screen.myModel>
			<#else>
				No model. An error has occurred.
				<#assign modelExists = false>
			</#if>
		
			<#if model.query??>
				<#assign query = model.query>
			<#else>
				<#assign query = "">
			</#if>
			
			<#assign allDataTypes = "__ALL__DATATYPES__SEARCH__KEY">
			<#--====================================-->	
		
			<#-- imports -->	
			<#import "../qtlfinder3/ResultList.ftl" as rl>
			<#import "../qtlfinder3/HumanDisease.ftl" as hd>
			<#import "../qtlfinder3/RegionSearch.ftl" as rs>
			<#import "../qtlfinder3/ComparePhenotypes.ftl" as cp>
			<#import "../qtlfinder3/QtlLoci.ftl" as ql>
			<#import "../qtlfinder3/OverlapResults.ftl" as or>
			<#import "../qtlfinder3/ShoppingCart.ftl" as sc>
			<#import "../qtlfinder3/MultiPlot.ftl" as mp>
			<#import "../qtlfinder3/CompareResults.ftl" as cr>
			
			<#-- macro's-->	
			<@diseaseMapping model = model screen = screen/>	
			<@browseSearch />
			
			<#if model.screenType == "humanDisease">
				<@hd.humanDisease model = model screen = screen />
			</#if>
			
			<#if model.screenType == "genomicRegion">
				<@rs.regionSearch model = model screen = screen />
			</#if>
			
			<#if model.screenType == "qtlLoci">
				<@ql.qtlLoci model = model screen = screen />
			</#if>
			
			<#if model.screenType == "comparePhenotypes">
				<@cp.comparePhenotypes model = model screen = screen /> 
				<@cr.compareResults model = model screen = screen />
			</#if>
		
			<#if model.screenType == "shoppingCart">
				<@sc.shoppingCart model = model screen = screen />
			</#if>
			
			<@rl.resultList model = model screen = screen/>
			
			<#if model.multiplot??>
				<@mp.multiPlot model=model screen=screen />
			</#if>
			
			<@styleAndScript />
		</div>
	</form>
</#macro>

<#macro diseaseMapping model screen>
	<div style="position:relative;float:left;">
		<select id="diseaseMapping" name="diseaseMapping">
			<option value="OMIM" <#if model.diseaseMapping == "OMIM">selected="selected"</#if>>OMIM</option>
			<option value="DGA" <#if model.diseaseMapping == "DGA">selected="selected"</#if>>DGA</option>
			<option value="GWAS" <#if model.diseaseMapping == "GWAS">selected="selected"</#if>>GWAS</option>
		</select>

		<button type="submit" id="search "onclick="document.forms.${screen.name}.__action.value = '__qtlfinderhd__mappingChange';document.forms.${screen.name}.submit();">Go</button>
	</div>	
</#macro>

<#macro browseSearch>
	<table align="center" id="browse">
		<tr>
			<td align="center" style="padding-left:0px;">
				<a href="molgenis.do?__target=QtlFinderHD&select=QtlFinderHD&screen=humanDisease"><b>Human Diseases</b></a>
			</td>	
			<td align="center">		
				<a href="molgenis.do?__target=QtlFinderHD&select=QtlFinderHD&screen=genomicRegion"><b>Genomic region</b></a>
			</td>	
			<td align="center">	
				<a href="molgenis.do?__target=QtlFinderHD&select=QtlFinderHD&screen=qtlLoci"><b>QTL Loci</b></a>
			</td>	
			<td align="center">
				<a href="molgenis.do?__target=QtlFinderHD&select=QtlFinderHD&screen=comparePhenotypes"><b>Compare Phenotypes</b></a>
			</td>	
		</tr>	
	</table>
	<hr style="border-color:#13507A;border-width:thin;">
</#macro>

<#macro styleAndScript>
<#--- SCRIPTS --->
	<script>
		$(document).ready(function(){
		
			<#-- TABLE WIDGET -->
			$( "#wormHumanTable" ).dataTable();
			
			<#-- DROPDOWN WIDGET -->
			$("#diseaseSelect").chosen();
			$("#humanPhenotype").chosen();
			$("#wormPhenotype").chosen();
		});		
	</script>
	
	<#-- RESULT TABLE STYLE -->
	<style> 
		table.dataTable tr.odd{
			background:#EAEAEA;
		}
		
		table.dataTable tr.even{
			 	background:#FFFFFF;
		}
		
		table.dataTable tr.even td.sorting_1 {
			background:#F0F0F0;
		}
		
		table.dataTable tr.odd td.sorting_1 {
			background:#D8D8D8;
		}
		
		#browse td {padding-left:15px;padding-top:5px;}
		#browse	a {color:#13507A;}      
		#browse a:visited {color:#13507A;}
		#browse a:hover {color:#4682b4;}
		#browse a:active {color:#0000FF;}
		
		#probeReport a {color:#13507A;}      
		#probeReport a:visited {color:#13507A;}
		#probeReport a:hover {color:#4682b4;}
		#probeReport a:active {color:#0000FF;}
		
		#probeReport a.tip {
			text-decoration: none
		}
		
		#probeReport a.tip:hover {
		    position: relative
		}
		
		#probeReport a.tip span {
		    display: none
		}
		
		#probeReport a.tip:hover span {
		    border: #c0c0c0 1px dotted;
		    padding: 5px 20px 5px 5px;
		    display: block;
		    z-index: 100;
		    background: url(../images/status-info.png) #f0f0f0 no-repeat 100% 5%;
		    left: 0px;
		    margin: 10px;
		    width: 400px;
		    position: absolute;
		    top: 10px;
		    text-decoration: none
		}
	</style>
</#macro>
