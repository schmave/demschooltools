import { useMemo } from 'react';
import { Avatar as MuiAvatar } from '@mui/material';
import Typography from './Typography';

// Simple hash function to deterministically select a color based on a string
const hashString = (str) => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }
  return Math.abs(hash);
};

const Avatar = (props) => {
  const { name, ...restOfProps } = props;

  const { initials, colors } = useMemo(() => {
    if (!name || name.length === 0) {
      return { initials: '', colors: { color: 'white', backgroundColor: 'black' } };
    }

    const initialsArray = name.split(' ');
    let newInitials = initialsArray[0].charAt(0);
    if (initialsArray.length > 1) {
      newInitials += initialsArray[initialsArray.length - 1].charAt(0);
    }

    const colorIndex = hashString(name) % avatarColors.length;
    return {
      initials: newInitials,
      colors: avatarColors[colorIndex],
    };
  }, [name]);

  if (initials === '') {
    return null;
  }

  return (
    <MuiAvatar {...restOfProps} style={{ backgroundColor: colors.backgroundColor, color: colors.color }}>
      <Typography>
        {initials}
      </Typography>
    </MuiAvatar>
  );
};

export default Avatar;


const avatarColors = [
    {
      backgroundColor: '#F55127',
      color: 'white',
    },
    {
      backgroundColor: '#0FCEF5',
      color: 'black',
    },
    {
      backgroundColor: '#F5B327',
      color: 'black',
    },
    {
      backgroundColor: '#02F5A7',
      color: 'black',
    },
    {
      backgroundColor: '#09091c',
      color: 'white'
    },
    {
      backgroundColor: '#b05b08',
      color: 'white'
    },
    {
      backgroundColor: '#D949D7',
      color: 'white',
    },
    {
      backgroundColor: '#FF4F9E',
      color: 'white',
    },
    {
      backgroundColor: '#FF866C',
      color: 'black',
    },
    {
      backgroundColor: '#FFC354',
      color: 'black',
    },
    {
      backgroundColor: '#F9F871',
      color: 'black',
    },
    {
      backgroundColor: '#9F9DD3',
      color: 'black',
    },
    {
      backgroundColor: '#EFEDFF',
      color: 'black',
    },
    {
      backgroundColor: '#BFA975',
      color: 'black',
    },
    {
      backgroundColor: '#BFA975',
      color: 'black',
    },
    {
      backgroundColor: '#FFEFCA',
      color: 'black',
    },
    {
      backgroundColor: '#847655',
      color: 'white'
    },
    {
      backgroundColor: '#464555',
      color: 'white'
    },
    {
      backgroundColor: '#ABA9BC',
      color: 'black'
    },
    {
      backgroundColor: '#E80038',
      color: 'white'
    },
    {
      backgroundColor: '#FF4966',
      color: 'white'
    },
    {
      backgroundColor: '#4F8987',
      color: 'white'
    },
    {
      backgroundColor: '#6AFBCF',
      color: 'black'
    },
    {
      backgroundColor: '#00D6F4',
      color: 'black'
    },
    {
      backgroundColor: '#D03400',
      color: 'white'
    },
    {
      backgroundColor: '#A8A6DD',
      color: 'black'
    },
    {
      backgroundColor: '#464555',
      color: 'white'
    }
  ];
