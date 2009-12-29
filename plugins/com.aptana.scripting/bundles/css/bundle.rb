require 'radrails'

# its ruby, so this just addscommands/snippets in bundle (or replaces those with same name)
# many ruby files could add to a single bundle
RadRails.current_bundle do |bundle|
  bundle.name = "Ruby on Rails"
  bundle.author = "Christopher Williams"
  bundle.copyright = <<END
� Copyright 2009 Aptana Inc. Distributed under GPLv3 and Aptana Source license.
END

  bundle.description = <<END
CSS bundle for RadRails 3
END

  bundle.git_repo = "git://github.com/aptana/css_bundle.git"

  # most commands install into a dedicated CSS menu
  bundle.menu "CSS" do |css_menu|
    # this menu should be shown when any of the following scopes is active:
    css_menu.scope = [ "source.css" ]
    
	  # command/snippet names must be unique within bundle and are case insensitive
    css_menu.command "Insert Color..."
    css_menu.command "Show as HTML"
    css_menu.command "Documentation for Property"
    css_menu.command "Validate Selected CSS"
    css_menu.command "CodeCompletion CSS Properties"
  end
end