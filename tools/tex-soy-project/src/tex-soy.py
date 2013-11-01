#!/usr/bin/python

# Quick and dirty LaTeX to soy converter - highly tailored to specific tex file inputs

import sys, getopt, re, logging, os
from sets import Set

#constants
REPLACE_WORD = '{replace_me}'
TITLE_INDICATOR = '%\maketitle'
TITLE_MARKUP = 'h3'
SECTION_MARKUP = 'h4'
SUB_SECTION_MARKUP = 'h5'
#NEW_PARAGRAPH_INDICATOR = 'NEW_PARAGRAPH'
NAMESPACE_PATH_START = 'rutherford'
IMAGE_PATH = '{$ij.proxyPath}/static/figures/'

# Utility Functions

# whitelist for initial pass (anything not specified of the format \\ABC{x} will be removed including x)
def generate_whitelist():
   whitelist = Set()
   whitelist.add(r'\\section{')
   whitelist.add(r'\\subsection\**{')
   whitelist.add(r'\\caption{')
   #whitelist.add(r'\\begin{')
   whitelist.add(r"\\begin{equation}")
   whitelist.add(r"\\begin{equation\*}")
   whitelist.add(r"\\end{equation}")
   whitelist.add(r"\\begin{eqnarray}")
   whitelist.add(r"\\end{eqnarray}")   
   whitelist.add(r"\\end{equation\*}")
   #whitelist.add(r"\\end{")
   whitelist.add(r"\\frac{")
   whitelist.add(r"\\includegraphics")
   whitelist.add(r"\\textsc{")
   whitelist.add(r"\\textbf{")
   whitelist.add(r"\\textit{")
   whitelist.add(r"\\mbox{")
   whitelist.add(r"\\boxed{")
   whitelist.add(r"\\noindent{")
   whitelist.add(r"\\begin{document}")
   whitelist.add(r"\\end{document}")
   whitelist.add(r"\\vtr{")
   whitelist.add(r"\\begin{tabular}")
   whitelist.add(r"\\end{tabular}")
   whitelist.add(r"\\Concepttitle{")
   whitelist.add(r"\\ref{")
   #whitelist.add(r"\\label{")
   whitelist.add(r"\\textrm{")
   whitelist.add(r"\\quarter{")
   whitelist.add(r"\\third{")
   whitelist.add(r"\\half{")
   whitelist.add(r"\\color{")
   whitelist.add(r"\\sin")
   whitelist.add(r"\\cos")
   whitelist.add(r"\\sqrt{")
   whitelist.add(r"\\times{")
   whitelist.add(r"\\\\")
   #questions whitelist
   whitelist.add(r"\\begin{problem}")
   whitelist.add(r"\\end{problem}")
   whitelist.add(r"\\begin{enumerate}")
   whitelist.add(r"\\end{enumerate}")
   whitelist.add(r"\\item")

   logging.debug("Whitelist: " + str(whitelist))
   return whitelist

# generates black list (these items will be removed after the whitelist has been executed)
def generate_blacklist():
   blacklist = Set()
   blacklist.add(r'\begin{document}')
   #blacklist.add(r'\end{document}')
   blacklist.add(r'\center ')
   blacklist.add(r'\centering')
   blacklist.add(r'\Large')
   blacklist.add(r'\large')
   blacklist.add(r'\huge')   
   blacklist.add(r'\bf')
   blacklist.add(r'\break')
   blacklist.add(r'\it')
   blacklist.add(r'\textit')
   blacklist.add(r'\textbf')
   blacklist.add(r'\hline')
   blacklist.add(r'\noindent')
   logging.debug("Blacklist: " + str(blacklist))
   return blacklist

# This is where simple find replace strings are added
def generate_conversion_dictionary():
   tex_to_html_opening_tag_dictionary = dict()
   tex_to_html_opening_tag_dictionary[r'\\Concepttitle']=r'<'+TITLE_MARKUP+'>'+REPLACE_WORD+'</'+TITLE_MARKUP+'>'
   tex_to_html_opening_tag_dictionary[r'\\section']=r'<'+SECTION_MARKUP+'>'+REPLACE_WORD+'</'+SECTION_MARKUP+'>'
   tex_to_html_opening_tag_dictionary[r'\\subsection']=r'<'+SUB_SECTION_MARKUP+'>'+REPLACE_WORD+'</'+SUB_SECTION_MARKUP+'>'
   tex_to_html_opening_tag_dictionary[r'\\caption']=r'<span class="caption">'+REPLACE_WORD+'</span>'
   tex_to_html_opening_tag_dictionary[r'\\begin{tabular}']=r'<table>'
   tex_to_html_opening_tag_dictionary[r'\\end{tabular}']=r'</table>'
   tex_to_html_opening_tag_dictionary[r'\\includegraphics\[.*?\]*']=r'<img src="'+IMAGE_PATH+REPLACE_WORD+'"/>\n'
   tex_to_html_opening_tag_dictionary[r'\\includegraphics\[.*?\]*']=r'<img src="'+IMAGE_PATH+REPLACE_WORD+'"/>\n'
   tex_to_html_opening_tag_dictionary[r'\\vtr']=r' $\mathit{\underline{\boldsymbol{'+REPLACE_WORD+'}}}$ '
   return tex_to_html_opening_tag_dictionary

def generate_simple_find_replace_dictionary():
   simple_f_r_dictionary = dict()
   simple_f_r_dictionary['\\half']='$\\frac{1}{2}$'
   simple_f_r_dictionary['\\third']='$\\frac{1}{3}$'
   simple_f_r_dictionary['\\quarter']='$\\frac{1}{4}$'
   simple_f_r_dictionary['\\color{red}']=''
   simple_f_r_dictionary['\\color{black}']=''
   simple_f_r_dictionary['\\nl']='<br/>'
   simple_f_r_dictionary['\\nll']='<br/>'
   return simple_f_r_dictionary


# Simple utility function to look for soy commands added by this script so that they are not interfered with.
def __contains_soy_command(line):
    soy_commands = Set()
    soy_commands.add("{template")
    soy_commands.add("{namespace")
    soy_commands.add("{/template")
    soy_commands.add("{$ij.proxyPath")
    soy_commands.add("{call")
    soy_commands.add("{/call}")
    soy_commands.add("{param")
    soy_commands.add("{literal")
    soy_commands.add("{/literal")

    for command in soy_commands:
        if(command in line):
            return True

    return False

# Utility method to give you the soy command to declare a namespace based on a file provided
def __get_namespace(file_of_interest):
   # TODO generate namespace based on tex file location
   # TODO refactor so that it isn't in such a random place
   path = os.path.abspath(file_of_interest)
   path = path.split('/') # assumes runs in linux at the moment
   namespace = ''
   tmpindex = 0
   for value in path:
        if(value == NAMESPACE_PATH_START):
            namespace = '{namespace ' + value.lower()
        elif(namespace != '' and tmpindex != len(path)-1): #need to ignore last element as its the actual filename
            namespace = namespace + '.' + value.lower()

        tmpindex+=1

   namespace = namespace + '}'

   return namespace

# Utility method to give you the soy command to declare a template based on a file provided
def __get_template(file_of_interest):
   filename = os.path.splitext(os.path.basename(file_of_interest))[0]
   
   template_comment= "/**\n\
 * " + filename + "\n\
 */"

   template = template_comment +'\n' +'{template .' + filename.lower() +'}'

   return template

# Removes any strings matching the blacklist
def remove_blacklist(string, blacklist):
   for value in blacklist:
      string = string.replace(value, '')

   if(string != ''):
      return string
   else:
      return ''

# Generates a regex based on the whitelist provided - regex returned is just an exclude based on the replaced token REPLACE_WORD
def get_reg_ex_from_whitelist(whitelist):
   regex_exclude_template = '(?!'+REPLACE_WORD+')'
   regex = ''

   for value in whitelist:
      regex = regex + regex_exclude_template.replace(REPLACE_WORD, value)

   return regex

# Returns string array/list of the file contents
def read_source_file_from_disk(inputfile):
   fin = open(inputfile)
   logging.info("Reading File: " + inputfile)

   file_string = list()
   for line in fin:
      file_string.append(line)
   
   fin.close()
   return file_string

# Help / usage output
def usage():
   print 'tex-soy.py -h | -q (to use additional question rules) | -t (to user traversal logic to go looking for .tex files in the <inputfile> directory path) | -i <inputfile> -o <outputfile>'

# Output string array to disk line by line
def write_string_array_to_file(string_list, outputfile):
    
    fout = open(outputfile, "w+")
    
    logging.info("Creating/Updating File: " + outputfile)
    
    for line in string_list:
        
        fout.write(line)

    fout.close()

# Step 1 - Strip random LaTeX commands
# Initial strip of latex content
# Ignores everything up until the \begin(document) command
# uses regex to remove all commands of the form xyz{abc} with optional [] between z and { - this will exclude whitelist sequences
# removes lines starting with % or any new lines before the first non-newline character
# outputs results as temporary output file.
# TODO this will need to be changed to do it all in memory rather than file io - it was used for debugging originally
def initial_strip_of_latex_commands(inputstring, whitelist, blacklist):
   index = 0
   doc_start_found = False

   outputstring = []

   for line in inputstring:
      if(line.startswith(r'\begin{document}') or line.startswith(r'\begin{problem}')):
         doc_start_found = True         

      # handle comments that are in the middle of lines
      if('%' in line and not '\%' in line and not TITLE_INDICATOR in line):
         line = line.split('%')[0]

      if(index >= 0 and doc_start_found):
         check_for_commands_with_braces = re.compile(get_reg_ex_from_whitelist(whitelist) + r'((\\[A-Za-z]*(\[.*?\])*\{(.)*\})*)')
         check_for_single_commands = re.compile(get_reg_ex_from_whitelist(whitelist) + r'(^\\[A-Za-z]*)')

         match1 = re.search(check_for_commands_with_braces, line)
         match2 = re.search(check_for_single_commands, line)
               
         if(match1 or match2):
            line = re.sub(check_for_commands_with_braces,"",line) #removes x{ unless in whitelist
            line = re.sub(check_for_single_commands,"",line) #removes \nl and \center etc
            
            line = remove_blacklist(line, blacklist)

         if((not line.startswith('%')) or line.startswith(TITLE_INDICATOR) or (index == 0 and not line.startswith('\r') or line.startswith('\n'))):
            outputstring.append(line)
            index += 1
   
   return outputstring

# Step 2 - Build structure of the document and add Soy specific commands
# This is the quick dirty conversion to a structured html fragment using string replacement techniques
# This function arranges for the soy commands to be included, attempts to detect when paragraphs should be started
# attempts to guess where sections start and finish as well as tables and equations.
def convert_to_soy(inputfile,temp_input_list,question_flag,conversion_dictionary):
   # apply special one-off rules to temp_input_list
   
   # Rule 1 \%maketitle will always appear immediately before the title
   tmpindex = 0
   for line in temp_input_list:
      
      if(line.startswith(TITLE_INDICATOR)):
         # the next line should have the title
         title = re.search(r'\{(.*?)\}', temp_input_list[tmpindex+1])

         if(title != None):

            temp_title = title.group(1)
            
            # remove extra random commands from the title that were missed due to the white list
            temp_title = remove_blacklist(temp_title, ['\\textbf{'])
            
            temp_title = '<'+TITLE_MARKUP+'>' + temp_title.strip() + '</'+TITLE_MARKUP+'>'

            temp_input_list[tmpindex] = temp_title
            
            #clear next line as we have moved it to this one
            temp_input_list[tmpindex+1] = ''

            break
         else:
            logging.error("Unable to find main title. The title indicator " + TITLE_INDICATOR + " should preceed the title of the form \\x{}.")
      tmpindex+=1

   # generate namespace based on tex file location
   namespace = __get_namespace(inputfile)

   template=__get_template(inputfile)

   soyheader = namespace + '\n\n' + template

   temp_input_list.insert(0, soyheader)

   # build structure to the document
   tmpindex = 0
   inparagraph = False
   inequation = False
   intable = False
   intablerow = False

   for line in temp_input_list:
      # do easy dictionary conversions
      for key in conversion_dictionary:
         match = re.search(key, line)
         if(match):
            m = re.search(r'\{(.*?)\}', line) # easy braces extraction
            line = conversion_dictionary[key].replace(REPLACE_WORD,m.group(1))

      # do easy find replace from simple dictionary
      simple_f_r_dictionary = generate_simple_find_replace_dictionary()
      for key in simple_f_r_dictionary:
        line = line.replace(key, simple_f_r_dictionary[key])

      # multi-line equations
      if('\\begin{eq' in line):
        line = line.replace('\\begin{equation*}', '$$')
        line = line.replace('\\begin{equation}', '$$')
        line = line.replace('\\begin{eqnarray}', '$$')
        inequation = True
      
      if('\\end{eq' in line):         
        line = line.replace('\\end{equation*}', '$$')
        line = line.replace('\\end{equation}', '$$')
        line = line.replace('\\end{eqnarray}', '$$')        
        inequation = False

      # tables
      if('<table>' in line):         
        intable = True
        # we may as well start a new row
        line = line + '\n<tr>'
        intablerow = True

      if('</table>' in line):         
        intable = False
        # we probably need to finish our last row
        intablerow = False

      if(intable):
            if(not intablerow and '&' in line):
                line = '<tr><td>'+line
                intablerow = True
            if(intablerow):
                line = line.replace('&', '</td> <td>')
                line = line.replace('\\\\', '</td></tr>')
                intablerow = False

      # paragraphs
      # if('\\nll' in line):
      #   line = line.replace('\\nll', NEW_PARAGRAPH_INDICATOR)
      # if('\\nl' in line):  
      #   line = line.replace('\\nl', NEW_PARAGRAPH_INDICATOR)

      if(not inequation and not intable and not question_flag):

          if(SECTION_MARKUP in line or TITLE_MARKUP in line or SUB_SECTION_MARKUP in line):
             # if(inparagraph):
             #    line = '</p>\n<p>' + line
             #    if(NEW_PARAGRAPH_INDICATOR in line):
             #        line = line.replace(NEW_PARAGRAPH_INDICATOR, '') #remove artificially inserted new paragraph indicator

             #create new paragraph 
             temp_input_list.insert(tmpindex+1,'\n<p>')
             inparagraph = True

          if('<p' in line):
             inparagraph = True

          if(line.endswith('</p>')):
             inparagraph = False

          if('\\\\' in line and inparagraph):
             line = line.replace('\\\\', '</p>' + '\n' + '<p>') 
          elif('\\\\' in line):
             inparagraph = True
             line = line.replace('\\\\', '\n' + '<p>') 
          elif('\end{document}' in line):
             line = '</p>\n{/template}'
             inparagraph = False

          #remove all random curly braces that may be in the text unless they are part of equations
          if(not inequation and '$' not in line and not __contains_soy_command(line)): #TODO bug because it doesnt detect inline equations properly
              line = line.replace('{','')
              line = line.replace('}','')
      
      if(((inequation and not inparagraph) or ('$' in line and not inparagraph)) and not question_flag):
        inparagraph = True
        line ='<p>' + line

      # deal with lb / rb soy issue
      if(not __contains_soy_command(line)):
          line = line.replace('{', '~lb~') #intermediate replace
          line = line.replace('}', '~rb~')
          line = line.replace('~lb~','{lb}') #final replace
          line = line.replace('~rb~','{rb}')

      # replace with image file extensions
      if('.eps' in line):
        line = line.replace('.eps', '.svg')

      temp_input_list[tmpindex] = line #write value to list for consistency
      tmpindex+=1

   return temp_input_list

def traverse(currentDir,searchext):
    infos = []
    for curdir, dirs, files in os.walk(currentDir): # Walk directory tree
        for f in files:
            if(str(f).split('.')[1] == searchext):
                infos.append(os.path.abspath(os.path.join(curdir, f)))
    return infos

def replace_braces_with_soy(stringinput):
    # deal with lb / rb soy issue
    if(not __contains_soy_command(stringinput)):
      stringinput = stringinput.replace('{', '~lb~') #intermediate replace
      stringinput = stringinput.replace('}', '~rb~')
      stringinput = stringinput.replace('~lb~','{lb}') #final replace
      stringinput = stringinput.replace('~rb~','{rb}')
    return stringinput

def strip_all_braces(stringinput):
    stringinput = stringinput.replace('{','')
    stringinput = stringinput.replace('}','')
    return stringinput

# Semantics are known only by line order unfortunately so this is very fragile!
def convert_questions_to_soy(inputfile, input_list):
    #use same whitelist and blacklist
    whitelist = generate_whitelist()
    blacklist = generate_blacklist()

    template_name = __get_template(inputfile)
    found_first_answer = False
    tmpindex = 0
    for line in input_list:
        if('\\item' in line):

            line = '[\'desc\':\'' + strip_all_braces(line.replace('\r\n','')) + '\']'
            
            if('\\item' not in input_list[tmpindex+1]):
                line = line + '\n'
            else:
                line = line + ',\n'

            line = line.replace('\\item','')
            line = line.replace('\\textrm','')
            if(found_first_answer == False):
                found_first_answer = True
                line = "{param type: 'checkbox' /}\n"+ "{param choices: [" + strip_all_braces(line)

        input_list[tmpindex] = line
        tmpindex+=1

    input_list = initial_strip_of_latex_commands(input_list,whitelist,blacklist)

    tmpindex = 0
    inanswerslist = False
    nextLineIsFooter = False
    footer =''
    explanation = ''
    footerindex = -1
    explanationindex = -1
    for line in input_list:

        #find question start / question end
        if('\\begin{problem}' in line):
            line = '\n'

        elif('\\end{problem}' in line):
            line = ''

        #find begin enumerate for answers
        if('\\begin{enumerate}' in line):
            line = '{call .mcq}\n'
            inanswerslist = True
        elif('\\end{enumerate}' in line):
            line = ']/}\n'+'{/call}\n'
            inanswerslist = False
            footer = '{call .questionFooter}\n{param footer}\n' + strip_all_braces(input_list[tmpindex+1]) + '{/param}\n{/call}\n'
            footerindex = tmpindex+1
            explanation = '{call .questionExplanation}\n{param explanation}\n' + strip_all_braces(input_list[tmpindex+2]) + '{/param}\n{/call}\n'
            explanationindex = tmpindex+2

        input_list[tmpindex] = line

        tmpindex+=1

    input_list[footerindex] = footer
    input_list[explanationindex] = explanation
    input_list.append('{/template}')

    input_list = convert_to_soy(inputfile, input_list, True, generate_conversion_dictionary())



    return input_list

# main method
def main(argv):
   logging.basicConfig(format='%(levelname)s - %(message)s', level=logging.INFO)

   inputfile = ''
   outputfile = ''
   traversal_directory = False
   questions = False
   concepts = True

   try:
      opts, args = getopt.getopt(argv,"hqcti:o:",["ifile=","ofile="])
   except getopt.GetoptError:
      usage()
      sys.exit(2)
   
   if len(opts) == 0:
      usage()
      sys.exit(2)

   for opt, arg in opts:
      if opt == '-h':
         usage()
         sys.exit()
      elif opt in ("-i", "--ifile"):
         inputfile = arg
      elif opt in ("-o", "--ofile"):
         outputfile = arg
      elif opt in ("-t", "--traverse"):
         traversal_directory = True
      elif opt in ("-c", "--concepts"):
         concepts = True
         questions = False
      elif opt in ("-q", "--questions"):
         questions = True
         concepts = False         

   if(traversal_directory and concepts):
       # locate all files to be included
       filelist = traverse(inputfile,'tex')

       whitelist = generate_whitelist()
       blacklist = generate_blacklist()
       conversion_dictionary = generate_conversion_dictionary()

       for item in filelist:
        logging.info('Processing: ' + item)
        tex_source_list = read_source_file_from_disk(os.path.abspath(item))

        tmpoutput = initial_strip_of_latex_commands(tex_source_list,whitelist,blacklist)

        finaloutput = convert_to_soy(item,tmpoutput,False,conversion_dictionary)

        outputpath = item.replace('.tex', '.soy')
        
        logging.info('Writing out: ' + outputpath)
        write_string_array_to_file(finaloutput, outputpath)
       
       logging.info("Batch processing complete")
       exit()
   elif(concepts):
       #build up data structures
       tex_source_list = read_source_file_from_disk(inputfile)

       #execute the program 
       tmpoutput = initial_strip_of_latex_commands(tex_source_list,generate_whitelist(), generate_blacklist())
       finaloutput = convert_to_soy(inputfile,tmpoutput,False,generate_conversion_dictionary())

       #output the answer to a file
       write_string_array_to_file(finaloutput, outputfile)
   elif(questions):
        # do question processing
        tex_source_list = read_source_file_from_disk(inputfile)
        tmpoutput_string_list = convert_questions_to_soy(inputfile,tex_source_list)
        outputfile = write_string_array_to_file(tmpoutput_string_list, outputfile)

if __name__ == "__main__":
   main(sys.argv[1:])

