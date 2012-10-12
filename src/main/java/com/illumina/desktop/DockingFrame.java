package com.illumina.desktop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.action.CAction;
import bibliothek.gui.dock.common.event.CControlListener;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.event.CFocusListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.event.DockableAdapter;

import com.illumina.desktop.lnf.IlluminaDockingTheme;

public class DockingFrame extends JFrame
{
    protected static Map<DefaultSingleCDockable, Component> componentMap = new ConcurrentHashMap<DefaultSingleCDockable, Component>();
    protected static DefaultSingleCDockable focusedDockable;
    private boolean focused;
    
    public DockingFrame()
    {
        super();
        this.addWindowFocusListener(new WindowFocusListener()
        {
            @Override
            public void windowGainedFocus(WindowEvent e)
            {
                focused = true;
            }

            @Override
            public void windowLostFocus(WindowEvent e)
            {
                focused = false;
            }
        });
        getDockingControl();
    }

    private JMenu windowMenu;
    public JMenu getWindowMenu()
    {
        if (windowMenu != null) return windowMenu;
        return windowMenu = createWindowMenu();
    }
    protected JMenu createWindowMenu()
    {
        windowMenu = new JMenu("Window");
        super.getJMenuBar().add(windowMenu);
        return windowMenu;
    }
    
    protected CControl dockingControl;
    public CControl getDockingControl()
    {
        if (dockingControl != null) return dockingControl;
        return dockingControl = createDockingControl();
    }

    @SuppressWarnings("deprecation")
    protected CControl createDockingControl()
    {
        dockingControl = new CControl(this);
        this.add(dockingControl.getContentArea(), BorderLayout.CENTER);
        dockingControl.setTheme(new IlluminaDockingTheme());

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if (evt.getPropertyName().equalsIgnoreCase("focusOwner") && evt.getNewValue() != null)
                {
                    if (Component.class.isAssignableFrom(evt.getNewValue().getClass()) && focusedDockable != null)
                    {
                        componentMap.put(focusedDockable, (Component) evt.getNewValue());
                    }
                }
            }

        });

        dockingControl.addFocusListener(new CFocusListener()
        {
            @Override
            public void focusGained(CDockable evt)
            {
                if (DefaultSingleCDockable.class.isAssignableFrom(evt.getClass()))
                {
                    DefaultSingleCDockable dockable = (DefaultSingleCDockable) evt;
                    if (focusedDockable != null && !focusedDockable.equals(dockable))
                    {
                        Component component = componentMap.get(dockable);
                        if (component != null) component.requestFocusInWindow();
                    }
                    focusedDockable = dockable;
                }
            }

            @Override
            public void focusLost(CDockable dockable)
            {

            }
        });
        return dockingControl;
    }
    
    public void saveDockingLayout(File path)
    {
        try
        {
            dockingControl.write(path);
        }
        catch (Throwable t)
        {
           t.printStackTrace();
        }
    }

    public void loadDockingLayout(File path)
    {
        try
        {
            if (!path.exists()) return;
            dockingControl.read(path);
        }
        catch (Throwable t)
        {
           t.printStackTrace();
        }
    }
    
    public <T extends Component> T getDockableContent(String id,Class<T>clazz)
    {
        DefaultSingleCDockable dockable = (DefaultSingleCDockable) getDockingControl().getSingleDockable(id);
        return clazz.cast(dockable.getContentPane().getComponent(0));
    }
    
    public DefaultSingleCDockable addDockable(final DockingContentProvider provider)
    {
        try
        {
            DefaultSingleCDockable dockable = (DefaultSingleCDockable) getDockingControl().getSingleDockable(
                    provider.getDockingId());
            if (dockable != null)
            {
                dockable.toFront();
                return dockable;
            }
            else
            {
                return createDockable(provider);
            }
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Error launching dockable: " + t.getMessage(), t);
        }
    }
    
    protected DefaultSingleCDockable createDockable(final DockingContentProvider provider)
    {
        DefaultSingleCDockable dockable = new DefaultSingleCDockable(provider.getDockingId(),
                provider.getDockingIcon(), provider.getDockingTitle(), provider.getDockingContent());

        addActionsToDockable(provider, dockable);
        dockable.setCloseable(true);
        getDockingControl().add(dockable);
        dockable.setVisible(true);
        addDockableViewMenuItem(dockable);
        return dockable;

    }
    

    protected void addDockableViewMenuItem(final DefaultSingleCDockable dockable)
    {

        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(dockable.getTitleText());
        menuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dockable.setVisible(menuItem.isSelected());
            }
        });
        
        getWindowMenu().add(menuItem);
        menuItem.setSelected(true);

        dockable.addCDockableStateListener(new CDockableAdapter()
        {
            @Override
            public void visibilityChanged(CDockable arg)
            {
                menuItem.setSelected(dockable.isVisible());
            }

        });

        dockable.intern().addDockableListener(new DockableAdapter()
        {

            @Override
            public void titleTextChanged(Dockable arg0, String old, String newName)
            {
                menuItem.setText(newName);
            }

        });

        getDockingControl().addControlListener(new CControlListener()
        {

            @Override
            public void added(CControl arg0, CDockable arg1)
            {

            }

            @Override
            public void closed(CControl arg0, CDockable arg1)
            {

            }

            @Override
            public void opened(CControl arg0, CDockable arg1)
            {

            }

            @Override
            public void removed(CControl arg0, CDockable arg1)
            {
                if (arg1 == dockable) getWindowMenu().remove(menuItem);
            }
        });

    }    
   

    
    protected void addActionsToDockable(final DockingContentProvider provider, final DefaultSingleCDockable dockable)
    {
        if (provider.getDockingActions() != null)
        {
            for (CAction action : provider.getDockingActions())
            {
                dockable.addAction(action);
            }
        }
    }
    
    public boolean isFocused()
    {
        return focused;
    }
}
