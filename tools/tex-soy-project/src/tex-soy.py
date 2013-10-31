#!/usr/bin/python

# Quick and dirty LaTeX to converter

import sys, getopt, re, logging, os
from sets import Set

#constants
REPLACE_WORD = '{replace_me}'
TITLE_INDICATOR = '%\maketitle'
TITLE_MARKUP = 'h3'
SECTION_MARKUP = 'h4'

def usage():
   print 'tex-soy.py -h | -i <inputfile> -o <outputfile>'

# whitelist for initial pass
def generate_whitelist():
   whitelist = Set()
   whitelist.add(r'\\section{')
   whitelist.add(r'\\caption{')
   #whitelist.add(r'\\begin{')
   whitelist.add(r"\\begin{equation}")
   whitelist.add(r"\\begin{equation\*}")
   whitelist.add(r"\\end{equation}")
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
   #whitelist.add(r"\\label{")
   whitelist.add(r"\\textrm{")
   whitelist.add(r"\\\\")
   logging.debug("Whitelist: " + str(whitelist))
   return whitelist

# generates remove list
def generate_blacklist():
   blacklist = Set()
   blacklist.add(r'\begin{document}')
   #blacklist.add(r'\end{document}')
   blacklist.add(r'\center ')
   blacklist.add(r'\centering')
   blacklist.add(r'\Large')
   blacklist.add(r'\bf')
   blacklist.add(r'\break')
   blacklist.add(r'\it')
   blacklist.add(r'\textit')
   blacklist.add(r'\noindent')
   logging.debug("Blacklist: " + str(blacklist))
   return blacklist

def generate_conversion_dictionary():
   tex_to_html_opening_tag_dictionary = dict()
   tex_to_html_opening_tag_dictionary[r'\\section']=r'<'+SECTION_MARKUP+'>'+REPLACE_WORD+'</'+SECTION_MARKUP+'>'
   tex_to_html_opening_tag_dictionary[r'\\caption']=r'<span class="caption">'+REPLACE_WORD+'</span>'
   tex_to_html_opening_tag_dictionary[r'\\begin{tabular}']=r'<table>'
   tex_to_html_opening_tag_dictionary[r'\\end{tabular}']=r'</table>'
   tex_to_html_opening_tag_dictionary[r'\\includegraphics\[.*?\]*']=r'<img src="'+REPLACE_WORD+'"/>\n'
   return tex_to_html_opening_tag_dictionary

# Removes any strings matching the blacklist
def remove_blacklist(string, blacklist):
   for value in blacklist:
      string = string.replace(value, '')

   if(string != ''):
      return string
   else:
      return ''

def get_reg_ex_from_whitelist(whitelist):
   regex_exclude_template = '(?!'+REPLACE_WORD+')'
   regex = ''

   for value in whitelist:
      regex = regex + regex_exclude_template.replace(REPLACE_WORD, value)

   return regex


 # Initial strip of latex content
 # Ignores everything up until the \begin(document) command
 # uses regex to remove all commands of the form xyz{abc} with optional [] between z and { - this will exclude whitelist sequences
 # removes lines starting with % or any new lines before the first non-newline character
 # outputs results as output file
def initial_strip_of_latex_commands(inputstring, outputfile, whitelist, blacklist):
   fin = inputstring

   fout = open(outputfile+'_tmp', "w+")
   logging.info("Creating/Updating File: " + outputfile+'_tmp')

   index = 0
   doc_start_found = False

   for line in fin:
      if(line.startswith(r'\begin{document}')):
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
            fout.write(line)
            index += 1
   
   fout.close()
   return outputfile+'_tmp'


def convert_to_soy(inputfile,temp_input_list,outputfile,conversion_dictionary):
   
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

   # TODO generate namespace based on tex file location
   # TODO refactor so that it isn't in such a random place
   path = os.path.abspath(inputfile)
   path = path.split('/') #assumes runs in linux
   namespace = ''
   tmpindex = 0
   for value in path:
        if(value == 'rutherford'):
            namespace = '{namespace ' + value
        elif(namespace != '' and tmpindex != len(path)-1):
            namespace = namespace + '.' + value

        tmpindex+=1

   namespace = namespace + '}'
   
   #add template info and required comment
   template_comment= "/**\n\
 * " + path[len(path)-1].split('.')[0] + "\n\
 */"

   namespace = namespace + '\n\n' + template_comment +'\n' +'{template .' + path[len(path)-1].split('.')[0]+'}'

   temp_input_list.insert(0, namespace)

   #convert known types to markup equivalent (e.g. section to h2's, double space to p tags, figures to image placeholders)
   fin = temp_input_list

   fout = open(outputfile, "w+")
   logging.info("Creating/Updating File: " + outputfile)

   tmpindex = 0
   inparagraph = False
   inequation = False
   intable = False

   for line in fin:

      # do easy dictionary conversions
      for key in conversion_dictionary:
         match = re.search(key, line)
         if(match):
            m = re.search(r'\{(.*?)\}', line) # easy braces extraction
            line = conversion_dictionary[key].replace(REPLACE_WORD,m.group(1))

      if('\\begin{equation' in line):
        line = line.replace('\\begin{equation*}', '$$')
        line = line.replace('\\begin{equation}', '$$')
        inequation = True
      
      if('\\end{equation' in line):         
        line = line.replace('\\end{equation*}', '$$')
        line = line.replace('\\end{equation}', '$$')
        inequation = False

      if('<table>' in line):         
        intable = True

      if('</table>' in line):         
        intable = False

      # paragraphs
      if(not inequation and not intable):

          if(SECTION_MARKUP in line or TITLE_MARKUP in line):
             if(inparagraph):
                line = '</p>\n' + line

             #create new paragraph 
             fin.insert(tmpindex+1,'\n<p>')
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

          #remove all random curly braces that may be in the text
          #line = line.replace('{','')
          #line = line.replace('}','')
      
      if("{template" not in line and '{namespace' not in line and '{/template' not in line):
        # deal with lb / rb soy issue
          line = line.replace('{', '~lb~')
          line = line.replace('}', '~rb~')
          line = line.replace('~lb~','{lb}')
          line = line.replace('~rb~','{rb}')

      fout.write(line)
      fin[tmpindex] = line #write value to list
      tmpindex+=1

   fout.close()

# Returns string of the file contents
def read_source_file_from_disk(inputfile):
   fin = open(inputfile)
   logging.info("Reading File: " + inputfile)

   file_string = list()
   for line in fin:
      file_string.append(line)
   
   fin.close()
   return file_string

# main method
def main(argv):
   logging.basicConfig(format='%(levelname)s - %(message)s', level=logging.INFO)

   inputfile = ''
   outputfile = ''
   try:
      opts, args = getopt.getopt(argv,"hi:o:",["ifile=","ofile="])
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

   #build up data structures
   tex_source_list = read_source_file_from_disk(inputfile)

   #execute the program - TODO fix unnecessary output to disk after debugging complete
   tmpoutput = initial_strip_of_latex_commands(tex_source_list,outputfile,generate_whitelist(), generate_blacklist())
   tmpoutput_source_list = read_source_file_from_disk(tmpoutput)
   
   logging.info("Removing temporary file from disk " + tmpoutput)
   os.remove(tmpoutput)
   
   finaloutput = convert_to_soy(inputfile, tmpoutput_source_list,outputfile,generate_conversion_dictionary())
   
if __name__ == "__main__":
   main(sys.argv[1:])

