import sys
from net.sf.chellow.monad import Monad
import os
import datetime

Monad.getUtils()['impt'](globals(), 'db', 'utils', 'templater')

files = []

if sys.platform.startswith('java'):
    download_path = Monad.getContext().getRealPath("/downloads")
else:
    download_path = os.path.join(os.environ['CHELLOW_HOME'], 'downloads')

for fl in os.listdir(download_path):
  statinfo = os.stat(os.path.join(download_path, fl))
  files.append(
        {
            'name': fl,
            'last_modified': datetime.datetime.utcfromtimestamp(
                statinfo.st_mtime),
            'size': statinfo.st_size,
            'creation_date': datetime.datetime.utcfromtimestamp(
                statinfo.st_ctime) })

templater.render(inv, template, {'files': files})